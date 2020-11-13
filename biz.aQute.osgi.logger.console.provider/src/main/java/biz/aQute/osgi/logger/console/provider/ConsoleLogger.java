package biz.aQute.osgi.logger.console.provider;

import java.util.Hashtable;

import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Will log to the console
 * 
 * <pre>
 * Order 			Object[] args = {entry,entry.getSequence(), entry.getTime(), entry.getBundle().getBundleId(), entry.getLogLevel(), entry.getException(), entry.getMessage(), entry.getLocation(), entry.getServiceReference(), entry.getThreadInfo()};
 * </pre>
 * 
 */

public class ConsoleLogger implements BundleActivator {
	private static final String							BIZ_A_QUTE_OSGI_LOGGER_CONSOLE_FORMAT	= "biz.aQute.osgi.logger.console.format";
	private static final String							DEFAULT_FORMAT							= "%s\n";
	ServiceTracker<LogReaderService, LogReaderService>	tracker;
	private boolean										mute									= false;
	private String										format;

	@Override
	public void start(BundleContext context) throws Exception {
		String format = System.getProperty(BIZ_A_QUTE_OSGI_LOGGER_CONSOLE_FORMAT);
		tracker.open();
		if (format != null) {
			if (format.equalsIgnoreCase("true")) {
				this.format = DEFAULT_FORMAT;
			} else {
				this.format = format;
			}
		}
		tracker = new ServiceTracker<LogReaderService, LogReaderService>(context, LogReaderService.class, null) {

			@Override
			public LogReaderService addingService(ServiceReference<LogReaderService> reference) {
				LogReaderService lr = super.addingService(reference);
				lr.addLogListener(ConsoleLogger.this::log);
				return lr;
			}
		};
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put("osgi.command.scope", "console");
		properties.put("osgi.command.function", new String[] { "mute", "unmute", "format" });
		context.registerService(ConsoleLogger.class, this, properties);

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		tracker.close();
	}

	@Descriptor("Mute the console logger")
	public void mute() {
		mute = true;
	}

	@Descriptor("Unmute the console logger")
	public void unmute() {
		mute = false;
	}

	@Descriptor("Temporarily set the format of the appender. Set system property "
			+ BIZ_A_QUTE_OSGI_LOGGER_CONSOLE_FORMAT + " to make it permanent")
	public void format(
			@Descriptor("String.format : order parameters is entry, sequence, time, bundle, level, exception, message, location, service ref, threadinfo") String format) {
		this.format = format;
	}

	private void log(LogEntry entry) {
		if (mute || format == null)
			return;

		Object[] args = { entry, entry.getSequence(), entry.getTime(), entry.getBundle().getBundleId(),
				entry.getLogLevel(), entry.getException(), entry.getMessage(), entry.getLocation(),
				entry.getServiceReference(), entry.getThreadInfo() };
		try {
			System.out.printf(format, args);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(entry);
		}
	}
}
