package biz.aQute.osgi.logger.components.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;

public class LogMessagesFormatterTest {

	private static final String DEFAULT_FORMAT = "%s:%04d %-5s %4s [%s,%s] %s%s%s";

	@SuppressWarnings({
		"rawtypes", "unchecked"
	})

	@Test
	public void test() throws Exception {
		LogMessageFormatter lf = new LogMessageFormatter(DEFAULT_FORMAT);

		LogEntry entry = mock(LogEntry.class);
		when(entry.getTime()).thenReturn(0L);
		when(entry.getSequence()).thenReturn(1L);

		Bundle bundle = mock(Bundle.class);
		when(bundle.getBundleId()).thenReturn(2L);
		when(entry.getBundle()).thenReturn(bundle);

		when(entry.getLogLevel()).thenReturn(LogLevel.WARN);
		when(entry.getLoggerName()).thenReturn("foo.bar.LoggerName");
		when(entry.getMessage()).thenReturn("MESSAGE");

		try {
			throw new Exception();
		} catch (Exception ee) {
			ee.fillInStackTrace();
			when(entry.getLocation()).thenReturn(ee.getStackTrace()[0]);
			when(entry.getException()).thenReturn(ee);
		}
		when(entry.getThreadInfo()).thenReturn("THREAD");

		ServiceReference<?> ref = mock(ServiceReference.class);
		when(ref.toString()).thenReturn("REFERENCE");
		when(entry.getServiceReference()).thenReturn((ServiceReference) ref);

		String format = lf.format(entry);
		assertThat(format).startsWith(
			"01/01 000000:0001 WARN     2 [THREAD,LoggerName] MESSAGEbiz.aQute.osgi.logger.components.provider.LogMessagesFormatterTest.test(LogMessagesFormatterTest.java:");
	}

	@Test
	public void testNulls() throws Exception {
		LogMessageFormatter lf = new LogMessageFormatter(DEFAULT_FORMAT);

		LogEntry entry = mock(LogEntry.class);
		when(entry.getTime()).thenReturn(0L);
		when(entry.getSequence()).thenReturn(1L);

		String format = lf.format(entry);
		assertThat(format).isEqualTo(
			"01/01 000000:0001 UNKNOWN      [,] ");
	}

	@Test
	public void testTimezoneIsUTC() throws Exception {
		LogMessageFormatter lf = new LogMessageFormatter("(z)" + DEFAULT_FORMAT);

		LogEntry entry = mock(LogEntry.class);
		when(entry.getTime()).thenReturn(0L);
		when(entry.getSequence()).thenReturn(1L);

		String format = lf.format(entry);
		assertThat(format).startsWith("UTC");
	}
}
