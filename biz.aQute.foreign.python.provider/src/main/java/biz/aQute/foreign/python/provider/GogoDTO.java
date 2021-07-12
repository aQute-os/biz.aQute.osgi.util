package biz.aQute.foreign.python.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.osgi.dto.DTO;

import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

public class GogoDTO extends Thread implements AutoCloseable {
	final static JSONCodec		codec			= new JSONCodec();
	final Exchange				in;
	final Exchange				out;
	final CommandSession		session;
	final ByteArrayOutputStream	outputBuffer	= new ByteArrayOutputStream();
	volatile long				lasttime;

	public GogoDTO(CommandProcessor processor) {
		in = new Exchange("fromPython");
		out = new Exchange("toPyton");
		this.session = processor.createSession(in, outputBuffer, System.err);
	}

	public static class ResultDTO extends DTO {
		public Object	value;
		public String	console;
		public String	error;
	}

	@Override
	public void run() {

		try {
			while (!isInterrupted()) {
				String line = readLine();
				if (line == null) {
					lasttime = Long.MAX_VALUE;
					return;
				}
				lasttime = System.currentTimeMillis();
				ResultDTO result = new ResultDTO();
				try {
					result.value = session.execute(line);
				} catch (Exception e) {
					result.error = e.getMessage();
				}
				result.console = new String(outputBuffer.toByteArray(), StandardCharsets.UTF_8);
				outputBuffer.reset();
				codec.enc().to(out).put(result).flush();
				out.append("\r\n");
			}
		} catch (IOException e) {
			// pipe closed
			return;
		} catch (Exception e) {
			if (isInterrupted())
				return;

			About.logger.warn("reading Python gogo input failed {}", e, e);
		} finally {
			About.logger.info("exiting python app gogo shell");
		}
	}

	private String readLine() throws InterruptedIOException {
		StringBuilder sb = new StringBuilder();
		while (true) {
			char ch = in.readChar();
			if (ch == '\n')
				return sb.toString();
			sb.append(ch);
		}
	}

	@Override
	public void close() throws Exception {
		interrupt();
		IO.close(in);
		IO.close(out);
	}

	public InputStream getStdin() {
		return out;
	}

	public Appendable getStdout() {
		return in;
	}

}
