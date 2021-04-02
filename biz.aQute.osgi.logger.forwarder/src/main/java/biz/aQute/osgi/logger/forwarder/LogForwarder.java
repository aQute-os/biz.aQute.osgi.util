package biz.aQute.osgi.logger.forwarder;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogService;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * Central controller for switching the {@link Facade} delegates to either a
 * queue as long as the OSGi LogService is not available, or if the OSGi
 * LogService is available, switch to forward log messages to the OSGi Logger
 * directly and furthermore drain the former queued log messages to it.
 */
public class LogForwarder implements ILoggerFactory {

	static final int							QUEUE_CAPACITY			= 5_000;
	static final Bundle							thisBundle				= FrameworkUtil.getBundle(LogForwarder.class);
	static final SecurityManagerContext			securityManagerContext	= new SecurityManagerContext();
	final BlockingQueue<Runnable>				queue					= new LinkedBlockingQueue<>(QUEUE_CAPACITY);
	static LogForwarder							SINGLETON				= new LogForwarder();
	static Instant								starting				= Instant.now();
	final ReferenceQueue<Facade>				referenceQueue			= new ReferenceQueue<>();

	// Guardby lock
	Object										lock					= new Object();
	final IdentityHashMap<LogService, Integer>	rankedLogServices		= new IdentityHashMap<>();
	final Map<LoggerId, FacadeReference>		facades					= new HashMap<>();
	LogService									selectedLogService		= null;

	class FacadeReference extends WeakReference<Facade> {

		final LoggerId name;

		FacadeReference(LoggerId name, Facade referent) {
			super(referent, referenceQueue);
			this.name = name;
		}

		void close() {
			synchronized (lock) {
				facades.remove(name);
			}
		}

	}

	/**
	 * Called from the StaticLoggerBinder
	 */
	public static ILoggerFactory getLoggerFactory() {
		return SINGLETON;
	}

	@Override
	public Logger getLogger(String name) {
		Bundle bundle = securityManagerContext.getCallerBundle();
		return createLogger(bundle, name);
	}

	private static class SecurityManagerContext extends SecurityManager {

		private Bundle getCallerBundle() {
			for (final Class<?> cc : getClassContext()) {
				final Bundle bundle = FrameworkUtil.getBundle(cc);
				if (bundle != null && !bundle.equals(thisBundle)) {
					return bundle;
				}
			}
			return thisBundle;
		}
	}

	static void addLogService(LogService newLogService, int ranking) {
		assert newLogService != null;
		synchronized (SINGLETON.lock) {

			Integer old = SINGLETON.rankedLogServices.put(newLogService, ranking);
			assert old == null;

			SINGLETON.setHighestRankingLogService();
		}
	}

	static void removeLogService(LogService oldLogService) {
		assert oldLogService != null;

		synchronized (SINGLETON.lock) {

			Integer remove = SINGLETON.rankedLogServices.remove(oldLogService);
			assert remove != null;

			if (SINGLETON.selectedLogService == oldLogService) {
				SINGLETON.setHighestRankingLogService();
			}
		}
	}

	Facade createLogger(Bundle bundle, String name) {
		synchronized (lock) {

			purge();

			LoggerId loggerId = new LoggerId(bundle, name);
			WeakReference<Facade> cachedFacadeLogger = facades.get(loggerId);

			if (cachedFacadeLogger != null) {
				Facade cachedLogger = cachedFacadeLogger.get();
				if (cachedLogger != null) {
					return cachedLogger;
				}
			}

			final Facade newLoggerFacade = new Facade(bundle, name);
			if (selectedLogService == null) {
				newLoggerFacade.delegate = new QueuingLogger(queue, newLoggerFacade);
			} else {
				setDelegate(newLoggerFacade, selectedLogService);
			}

			FacadeReference reference = new FacadeReference(loggerId, newLoggerFacade);
			facades.put(loggerId, reference);

			return newLoggerFacade;
		}
	}

	// guarded by lock
	private void purge() {
		FacadeReference fr = (FacadeReference) referenceQueue.poll();
		while (fr != null) {
			facades.remove(fr.name);
			fr = (FacadeReference) referenceQueue.poll();
		}
	}

	// guarded by lock
	private void setLogService(LogService newLogService) {

		if (selectedLogService == newLogService)
			return;

		selectedLogService = newLogService;
		if (newLogService == null) {
			// Start queuing
			getLoggerFacades().forEach(facade -> facade.delegate = new QueuingLogger(queue, facade));
		} else {
			selectedLogService.getLogger(LogForwarder.class.getName())
				.audit("RESTART {} #queued={}", starting, queue.size());
			// Start logging
			getLoggerFacades().forEach(facade -> {
				setDelegate(facade, newLogService);
			});
			queue.forEach(Runnable::run);
			queue.clear();
		}
	}

	/*
	 * Apache Felix will throw an IllegalArgumentException if a non-active
	 * bundle tries to log, e.g. in its bundle-activator method, and a NPE if
	 * the bundleContext is not yet set but the bundle is active
	 */
	// guarded by lock
	private static void setDelegate(Facade lf, LogService logService) {
		Bundle bundle = lf.getBundle();
		boolean isBundleInvalid = bundle == null || bundle.getBundleContext() == null;
		if (isBundleInvalid) {
			lf.delegate = logService.getLogger(lf.getName());
		} else {
			lf.delegate = logService.getLogger(lf.getBundle(), lf.getName(), org.osgi.service.log.Logger.class);
		}
	}

	// guarded by lock
	private Stream<Facade> getLoggerFacades() {
		return facades.values()
			.stream()
			.map(WeakReference::get)
			.filter(Objects::nonNull);
	}

	// guarded by lock
	private void setHighestRankingLogService() {
		Optional<LogService> highest = rankedLogServices.entrySet()
			.stream()
			.sorted((a, b) -> b.getValue()
				.compareTo(a.getValue()))
			.map(Map.Entry::getKey)
			.findFirst();

		setLogService(highest.orElse(null));
	}

	static void publish(LogRecord record) {
		SINGLETON.publish0(record);
	}

	private void publish0(LogRecord record) {
		if (record == null)
			return;

		String name = record.getLoggerName();
		if (name == null)
			name = "unknown.jul.logger";

		Bundle bundle = securityManagerContext.getCallerBundle();
		Facade logger = createLogger(bundle, name);

		logger.publish(record);
	}

}
