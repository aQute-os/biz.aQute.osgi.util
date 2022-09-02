package biz.aQute.aggregate.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.BundleTracker;

import biz.aQute.aggregate.api.Aggregate;
import biz.aQute.aggregate.api.AggregateImplementation;
import biz.aQute.aggregate.provider.TrackedBundle.ServiceInfo;
import biz.aQute.hlogger.util.HLogger;
import biz.aQute.osgi.concurrency.util.InitClose;
import biz.aQute.osgi.framework.util.FrameworkStartedDetector;
import biz.aQute.osgi.framework.util.FrameworkStartedDetector.Reason;

/**
 * A component that will analyze the active bundles for how many services they
 * will register and finds special services that are an Aggregate<S> over these
 * services.
 */
@SuppressWarnings({
	"rawtypes", "unchecked"
})
@Component(property = "condition=true", service = AggregateState.class)
@AggregateImplementation
public class AggregateState {
	final static Class					ARCHETYPE		= Aggregate.class;
	final static HLogger				logger			= HLogger.root(AggregateState.class.getSimpleName());
	final BundleContext					context;
	final BundleTracker<AutoCloseable>	tracker;
	final Map<Class, TrackedService>	trackedServices	= new HashMap<Class, TrackedService>();
	final List<TrackedBundle>			bundles			= new CopyOnWriteArrayList<>();
	final ScheduledExecutorService		executor		= Executors.newSingleThreadScheduledExecutor();
	final FrameworkStartedDetector		fsd;
	final InitClose						opentracker		= new InitClose(this::init, true);
	volatile boolean					inited;
	boolean								closed;

	@Activate
	public AggregateState(BundleContext context) {
		this.context = context;
		this.fsd = new FrameworkStartedDetector(context);
		this.tracker = new BundleTracker<AutoCloseable>(context, Bundle.ACTIVE, null) {
			@Override
			public AutoCloseable addingBundle(Bundle bundle, BundleEvent event) {
				return add(bundle);
			}

			@Override
			public void removedBundle(Bundle bundle, BundleEvent event, AutoCloseable tb) {
				AggregateState.close(tb);
			}

		};
		executor.execute(opentracker);
	}

	AutoCloseable init() {
		logger.debug("starting initialization");
		Reason reason = fsd.waitForStart();
		if (reason == Reason.INTERRUPTED) {
			logger.debug("interrupted");
			return () -> {};
		}
		logger.debug("framework started with %s", reason);

		tracker.open();
		inited = true;
		logger.debug("tracker opened");
		return () -> tracker.close();
	}

	@Deactivate
	void close() throws Exception {
		synchronized (this) {
			closed = true;
		}
		logger.debug("closing");
		close(opentracker);
		trackedServices.values()
			.forEach(TrackedService::close);
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		logger.debug("closed");
	}

	synchronized AutoCloseable add(Bundle bundle) {
		if (closed) {
			return null;
		}

		TrackedBundle trackedBundle = TrackedBundle.create(bundle);
		if (trackedBundle == null)
			return null;
		bundles.add(trackedBundle);
		trackedBundle.logger.debug("started");

		for (ServiceInfo info : trackedBundle.infos.values()) {
			TrackedService trackedService = trackedServices.computeIfAbsent(info.serviceType,
				st -> new TrackedService(this, st));

			trackedService.add(bundle, info);
		}
		return () -> remove(trackedBundle);
	}

	private synchronized void remove(TrackedBundle trackedBundle) {
		if (closed)
			return;

		bundles.remove(trackedBundle);
		trackedBundle.logger.debug("stopped");
		for (ServiceInfo info : trackedBundle.infos.values()) {
			TrackedService trackedService = trackedServices.get(info.serviceType);
			if (trackedService.remove(trackedBundle.bundle, info)) {
				logger.debug("purging %s", trackedService);
				TrackedService removed = trackedServices.remove(trackedService.serviceType);
				removed.close();
			}
		}
		trackedBundle.close();
	}

	void schedule(Runnable run, long delay) {
		logger.debug("scheduled");
		synchronized (this) {
			if (!closed) {
				if (delay == 0)
					executor.execute(run);
				else
					executor.schedule(run, delay, TimeUnit.MILLISECONDS);
				return;
			}
		}
		assert false : "should not schedule stuff after close";
	}

	boolean holdsLock() {
		return Thread.holdsLock(this);
	}

	static void close(AutoCloseable tb) {
		try {
			if (tb != null)
				tb.close();
		} catch (Exception e) {
			// ignore
		}

	}

}
