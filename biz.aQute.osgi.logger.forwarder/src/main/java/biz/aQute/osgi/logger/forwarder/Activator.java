package biz.aQute.osgi.logger.forwarder;

import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This acts as controller for the {@link Facade} instances to notify them if
 * the LogService became available or unavailable.
 * <p/>
 * If the {@link LogService} is registered to the OSGi ServiceRegistry, the
 * reference gets satisfied and this service component will activate. If the
 * {@link LogService} is not registered to the OSGi ServiceRegistry, the
 * reference cannot be satisfied and this service component will either not
 * activate, or if already active, will deactivate.
 */
public final class Activator implements BundleActivator {

	private ServiceTracker<LogService, LogService>	tracker;
	private final Handler							handler	= new Handler() {

																@Override
																public void publish(LogRecord record) {
																	LogForwarder.publish(record);
																}

																@Override
																public void flush() {}

																@Override
																public void close() {}
															};

	@Override
	public void start(final BundleContext bundleContext) throws Exception {

		LogManager.getLogManager()
			.getLogger("")
			.addHandler(handler);

		tracker = new ServiceTracker<LogService, LogService>(bundleContext, LogService.class, null) {
			@Override
			public LogService addingService(final ServiceReference<LogService> reference) {
				LogService newLogService = super.addingService(reference);
				Integer ranking = (Integer) reference.getProperty(Constants.SERVICE_RANKING);
				LogForwarder.addLogService(newLogService, ranking == null ? 0 : ranking);
				return newLogService;
			}

			@Override
			public void removedService(final ServiceReference<LogService> reference, final LogService oldLogService) {
				LogForwarder.removeLogService(oldLogService);
			}
		};
		tracker.open();
	}

	@Override
	public void stop(final BundleContext bundleContext) throws Exception {
		LogManager.getLogManager()
			.getLogger("")
			.removeHandler(handler);
		tracker.close();
	}

}
