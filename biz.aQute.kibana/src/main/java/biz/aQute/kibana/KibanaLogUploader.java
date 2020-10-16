package biz.aQute.kibana;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.osgi.dto.DTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import aQute.lib.base64.Base64;
import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

@Designate(ocd = KibanaLogUploader.Configuration.class, factory = true)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = "biz.aQute.kibana")
public class KibanaLogUploader extends Thread {
	private static final char MAGIC_CHAR_FOR_AT = '\u1234';
	final static long MAX_DELAY = 5000;

	@ObjectClassDefinition(description = "Kibana upload configuration")
	static public @interface Configuration {
		@AttributeDefinition(required = true, description = "List of URIs to the Elastic search host. This is the scheme + host + port. The path is discarded")
		String[] hosts();

		@AttributeDefinition(required = true, description = "Password for Elastic search")
		String password();

		@AttributeDefinition(required = true, description = "User id for Elastic search")
		String userid();

		@AttributeDefinition(min = "5", description = "Buffering delay in seconds before records are pushed")
		int delay() default 30;

		@AttributeDefinition(description = "The log index to use")
		String index();
	}

	public static class LogDTO extends DTO {

		// Kibana insists on a `@timestamp` property. This is not
		// supported by JSONCodec, so we use an Ethiopic unicode char
		// and replace it later with `@`

		public long		\u1234timestamp;
		public long		sequence;
		public long		bundleId;
		public LogLevel	level;
		public String	serviceReference;
		public String	message;
		public String	exceptionMessage;
		public String	exceptionClass;
		public String	loggerName;
		public String	threadInfo;
		public String	location;
	}

	final BlockingQueue<LogEntry>	queue	= new ArrayBlockingQueue<>(1000);
	final JSONCodec					codec	= new JSONCodec();
	final List<URI>					uris	= new ArrayList<>();
	final String					authorizationHeader;
	final String					index;
	private long					delay;

	@Activate
	public KibanaLogUploader(@Reference LogReaderService reader, Configuration configuration)
			throws URISyntaxException {
		super("aQute.kibana");
		reader.addLogListener(this::log);
		this.index = configuration.index();
		this.delay = configuration.delay() * 1000;
		for (String s : configuration.hosts()) {
			uris.add(new URI(s));
		}
		byte[] token = (configuration.userid() + ":" + configuration.password()).getBytes(StandardCharsets.UTF_8);
		this.authorizationHeader = "Basic " + Base64.encodeBase64(token);
		start();
	}

	@Deactivate
	void quit() {
		interrupt();
	}

	@Override
	public void run() {
		long deadline = Long.MAX_VALUE;
		List<LogDTO> dtos = new ArrayList<>();
		int failures = 0;

		try {

			while (!isInterrupted())
				try {
					LogEntry take = queue.poll(1000, TimeUnit.MILLISECONDS);
					if (take != null) {
						if (take.getTime() + delay < deadline) {
							deadline = take.getTime() + delay;
						}
						dtos.add(toDTO(take));
					} else {
						if (dtos.size() > 500 || System.currentTimeMillis() >= deadline) {
							if (flush(dtos)) {
								dtos.clear();
								failures = 0;
								deadline = Long.MAX_VALUE;
							}
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
			dto.\u1234timestamp = take.getTime();
			dto.bundleId = take.getBundle().getBundleId();
			dto.level = take.getLogLevel();
			dto.loggerName = take.getLoggerName();
			dto.message = take.getMessage();
			dto.threadInfo = take.getThreadInfo();
			dto.loggerName = take.getLoggerName();

			if (take.getServiceReference() != null) {
				dto.serviceReference = take.getServiceReference().toString();
			}

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

	private boolean flush(List<LogDTO> dtos) throws Exception {
		try {
			StringBuilder sb = new StringBuilder();
			for (LogDTO dto : dtos) {
				sb.append("{ \"create\": { \"_index\": \"" + index + "\" }}\n");
				codec.enc().to(sb).put(dto).close();
				sb.append("\n");
			}
			for (int i=0; i<sb.length();i++) {
				if ( sb.charAt(i) == MAGIC_CHAR_FOR_AT) {
					sb.setCharAt(i, '@');
				}
			}
			byte[] payload = sb.toString().getBytes(StandardCharsets.UTF_8);

			for (URI uri : uris) {
				if (post(uri, payload, "application/x-ndjson"))
					return true;
			}
			error("Could not send the payload to any of: %s", uris);
		} catch (Exception e) {
			error("Failed to flush %s", e);
		}
		return false;
	}

	private boolean post(URI uri, byte[] payload, String contenttype) {
		try {
			URL url = uri.resolve("/" + index + "/_bulk").toURL();

			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setInstanceFollowRedirects(true);
			con.addRequestProperty("Authorization", authorizationHeader);
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", contenttype);
			con.setRequestProperty("Content-Length", Integer.toString(payload.length));
			IO.copy(payload, con.getOutputStream());
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
}
