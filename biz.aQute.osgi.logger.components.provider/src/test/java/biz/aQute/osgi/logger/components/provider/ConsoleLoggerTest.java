package biz.aQute.osgi.logger.components.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogReaderService;

import aQute.lib.io.ByteBufferOutputStream;
import biz.aQute.osgi.logger.components.config.ConsoleLoggerConfiguration;

public class ConsoleLoggerTest {
	Mocks m = new Mocks();

	@Test
	public void test() throws Exception {
		LogReaderService lrs = m.getLogReaderService();
		ConsoleLoggerConfiguration cac = m.getMap(ConsoleLoggerConfiguration.class, "LOG_FORMAT",
			"%s:%04d %-5s %4s [%s,%s] %s%s%s", "LOG_TO_CONSOLE", "DEBUG");

		ConsoleLogger l = new ConsoleLogger(lrs, cac);

		ByteBufferOutputStream bbos = new ByteBufferOutputStream();
		try (PrintStream ps = new PrintStream(bbos)) {
			l.out = ps;

			LogEntry entry = mock(LogEntry.class);
			when(entry.getTime()).thenReturn(0L);
			when(entry.getSequence()).thenReturn(100L);
			when(entry.getLogLevel()).thenReturn(LogLevel.ERROR);

			l.log(entry);
			when(entry.getSequence()).thenReturn(101L);
			l.log(entry);
			ps.flush();

			String m = new String(bbos.toByteArray(), StandardCharsets.UTF_8);
			assertThat(m)
				.isEqualTo("01/01 00:00:00 ERROR  [] \n"
						+ "01/01 00:00:00 ERROR  [] \n");
		}
	}

}
