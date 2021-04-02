package biz.aQute.osgi.logger.components.provider;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogReaderService;

import aQute.lib.converter.Converter;

class Mocks {
	final static boolean inDebugger = java.lang.management.ManagementFactory.getRuntimeMXBean()
		.getInputArguments()
		.toString()
		.indexOf("-agentlib:jdwp") > 0;

	LogReaderService getLogReaderService(List<LogEntry> entry) {
		if (entry == null)
			entry = Collections.emptyList();
		LogReaderService lrs = mock(LogReaderService.class);
		when(lrs.getLog()).thenReturn(Collections.enumeration(entry));
		return lrs;
	}

	public LogReaderService getLogReaderService() {
		return getLogReaderService(null);
	}

	<T> T getMap(Class<T> type, String... args) throws Exception {
		if (isOdd(args.length))
			throw new IllegalArgumentException();

		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < args.length; i += 2) {
			map.put(args[i], args[i + 1]);
		}
		return Converter.cnv(type, map);
	}

	private static boolean isOdd(int l) {
		return l / 2 * 2 != l;
	}

	BundleContext getContext() {
		BundleContext cctx = mock(BundleContext.class);
		return cctx;
	}

	long	n		= 1000;
	long	time	= 0;

	LogEntry entry(String msg, LogLevel level, String name) {
		LogEntry entry = mock(LogEntry.class);
		when(entry.getTime()).thenReturn(0L);
		when(entry.getSequence()).thenReturn(n++);

		Bundle bundle = mock(Bundle.class);
		when(bundle.getBundleId()).thenReturn(2L);
		when(entry.getBundle()).thenReturn(bundle);

		when(entry.getLogLevel()).thenReturn(level);
		when(entry.getLoggerName()).thenReturn(name == null ? "foo.bar.LoggerName" : name);
		when(entry.getMessage()).thenReturn(msg);

		return entry;
	}

	public static Clock getClock(long time) {
		return Clock.fixed(Instant.ofEpochMilli(time), ZoneId.of("UTC"));
	}

	public int to(int n) {
		if (inDebugger)
			return Integer.MAX_VALUE;
		else
			return n;
	}

	public static void await(int timeoutInMs, BooleanSupplier f, String message) {
		long deadline = System.currentTimeMillis() + timeoutInMs;
		do {
			if ( f.getAsBoolean() )
				return;
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread()
					.interrupt();
				return;
			}
		} while (inDebugger || System.currentTimeMillis() < deadline);
		fail(message);
		return;
	}

	public static void await(int i, BooleanSupplier f) {
		await(i, f, "awaiting timed out");
	}

}
