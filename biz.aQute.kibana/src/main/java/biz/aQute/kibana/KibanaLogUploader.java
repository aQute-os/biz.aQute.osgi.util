package biz.aQute.kibana;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import aQute.lib.base64.Base64;
import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

//new HttpHost("16457eb61dd34826b6463db0c8aae5f8.us-east-1.aws.found.io", 9243, "https"))
// https://16457eb61dd34826b6463db0c8aae5f8.us-east-1.aws.found.io:9243/logs-foo/_bulk
// di17DinEhnHERcm8FqeVYZXx
@Designate(ocd = KibanaLogUploader.Configuration.class, factory = true)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class KibanaLogUploader extends Thread {
	final static long MAX_DELAY = 5000;

	@ObjectClassDefinition
	static public @interface Configuration {
		String[] hosts();

		String password();
	}

	public static class LogDTO extends DTO {
		public long					sequence;
		public long					time;
		public long					bundleId;
		public LogLevel				level;
		public ServiceReferenceDTO	serviceReference;
		public String				message;
		public String				exceptionMessage;
		public String				exceptionClass;
		public String				loggerName;
		public String				threadInfo;
		public String				location;
	}

	final BlockingQueue<LogEntry>	queue	= new ArrayBlockingQueue<>(1000);
	final JSONCodec					codec	= new JSONCodec();
	final List<URI>					uris	= new ArrayList<>();
	final String					authorizationHeader;

	@Activate
	public KibanaLogUploader(@Reference LogReaderService reader, Configuration configuration)
			throws URISyntaxException {
		reader.addLogListener(this::log);

		for (String s : configuration.hosts()) {
			uris.add(new URI(s));
		}
		byte[] token = ("elastic:" + configuration.password()).getBytes(StandardCharsets.UTF_8);
		this.authorizationHeader = "Basic " + Base64.encodeBase64(token);
		start();
	}

	@Deactivate
	void quit() {
		interrupt();
	}

	public void run() {
		long deadline = Long.MAX_VALUE;
		List<LogDTO> dtos = new ArrayList<>();
		int failures = 0;

		try {

			while (!isInterrupted())
				try {
					LogEntry take = queue.poll(deadline, TimeUnit.MILLISECONDS);
					if (LogLevel.WARN.implies(take.getLogLevel())
							&& take.getTime() + MAX_DELAY < deadline) {
						deadline = take.getTime() + MAX_DELAY;
					}
					dtos.add(toDTO(take));
					if (dtos.size() > 100 || System.currentTimeMillis() < deadline) {
						if (flush(dtos)) {
							dtos.clear();
							failures = 0;
						}
					}
				} catch (InterruptedException e) {
					this.interrupt();
					return;
				} catch (Exception e) {
					try {
						failures++;
						if (dtos.size() >= 1000) {
							dtos.removeIf(l -> LogLevel.WARN.implies(l.level));
							if (dtos.size() > 500)
								dtos.subList(500, dtos.size()).clear();
						}
						Thread.sleep(1000 * failures);
					} catch (InterruptedException ee) {
						this.interrupt();
						return;
					} catch (Exception e1) {
						error("Impossible %s", e1);
					}
				}

		} catch (Throwable e) {
			error("Fatal error %s, giving up", e);
		} finally {
			if (!isInterrupted())
				error("Exiting log kibana forwarder due to failure");
			else
				System.out.println("exiting kibana");
		}
	}

	private LogDTO toDTO(LogEntry take) {
		LogDTO dto = new LogDTO();
		try {
			dto.sequence = take.getSequence();
			dto.time = take.getTime();
			dto.bundleId = take.getBundle().getBundleId();
			dto.level = take.getLogLevel();
			dto.loggerName = take.getLoggerName();
			dto.message = take.getMessage();
			dto.threadInfo = take.getThreadInfo();
			dto.loggerName = take.getLoggerName();

			dto.serviceReference = toDTO(take.getServiceReference());

			Throwable exception = take.getException();
			if (exception != null) {
				dto.exceptionClass = exception.getClass().getName();
				dto.exceptionMessage = exception.getMessage();
			}

			StackTraceElement location = take.getLocation();
			if (location != null) {
				dto.location = location.toString();
			}

		} catch (Exception e) {
			dto.message = "CONVERSION FAILURE IN ->KIBANA " + e.toString();
			e.printStackTrace();
		}
		return dto;
	}

	private ServiceReferenceDTO toDTO(ServiceReference<?> ref) {
		if (ref == null)
			return null;

		ServiceReferenceDTO sr = new ServiceReferenceDTO();
		sr.id = (long) ref.getProperty(Constants.SERVICE_ID);
		sr.bundle = ref.getBundle().getBundleId();
		if (ref.getUsingBundles() != null) {
			sr.usingBundles = Stream.of(ref.getUsingBundles()).mapToLong(Bundle::getBundleId).toArray();
		}
		sr.properties = new LinkedHashMap<>();
		for (String k : ref.getPropertyKeys()) {
			sr.properties.put(k, ref.getProperty(k));
		}
		return sr;
	}

	private boolean flush(List<LogDTO> dtos) throws Exception {
		try {
			StringBuilder sb = new StringBuilder();
			for (LogDTO dto : dtos) {
				sb.append("{ \"index\": {}}\n");
				codec.enc().to(sb).put(dto).close();
				sb.append("\n");
			}

			byte[] payload = sb.toString().getBytes(StandardCharsets.UTF_8);

			for (URI uri : uris) {
				if (post(uri, payload))
					return true;
			}
			error("Could not send the payload to any of: %s", uris);
		} catch (Exception e) {
			error("Failed to flush %s", e);
		}
		return false;
	}

	private boolean post(URI uri, byte[] jsonpayload) {
		try {
			URL url = uri.toURL();

			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setInstanceFollowRedirects(true);
			con.addRequestProperty("Authorization", authorizationHeader);
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json;charset='UTF-8'");
			con.setRequestProperty("Content-Length", Integer.toString(jsonpayload.length));
			IO.copy(jsonpayload, con.getOutputStream());
			con.connect();
			int responseCode = con.getResponseCode();
			if (responseCode == 201 || responseCode == 200)
				return true;

			error("Not an ok response from posting payload: %s : %s %s", uri, con.getResponseMessage(),
					con.getResponseCode());
		} catch (Exception e) {
			error("Failed to send the payload: %s : %s", uri, e);
		}
		return false;
	}

	private void error(String format, Object... args) {
		try {
			for (int i = 0; i < args.length; i++) {
				args[i] = Converter.cnv(String.class, args[i]);
			}
			String message = String.format(format, args);
			System.err.println(message);
		} catch (Exception e) {
			System.err.println(format + " " + Arrays.toString(args));
		}
	}

	private void log(LogEntry entry) {
		boolean ok = queue.add(entry);
		if (!ok) {
			System.err.println(entry);
		}
	}

	// @Before
	// public void setup() {
	// }
	//
	// @Test
	// public void post() throws IOException {
	// Request request = new Request("POST", "/logs-foo/_doc");
	// request.setJsonEntity("{\"date\":" + System.currentTimeMillis()
	// +
	// ",\"thread\":\"[t1]\",\"level\":\"DEBUG\",\"logger\":\"LOGGER\",\"message\":\"msg\"}");
	// Response response = client.performRequest(request);
	// System.out.println(response);
	// }
	//
	// @Test
	// public void get() throws IOException {
	// Request request = new Request("GET",
	// "/logs-foo/_doc/dQZh_nQBKVTtM-kte3_1");
	// Response response = client.performRequest(request);
	// String collect = IO.collect(response.getEntity().getContent());
	// System.out.println(collect);
	// }
	//
	// @Test
	// public void search() throws IOException {
	// Request request = new Request("GET", "/logs-foo/_search");
	// Response response = client.performRequest(request);
	// String collect = IO.collect(response.getEntity().getContent());
	// System.out.println(collect);
	// }
}
