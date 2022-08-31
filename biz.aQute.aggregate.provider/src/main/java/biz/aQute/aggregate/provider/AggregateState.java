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

import biz.aQute.aggregate.provider.FrameworkStartedDetector.Reason;
import biz.aQute.aggregate.provider.TrackedBundle.ServiceInfo;

/**
 * A component that will analyze the active bundles for how many services they
 * will register and finds special services that are an Iterable over these
 * services.
 */
@SuppressWarnings({
	"rawtypes", "unchecked"
})
@Component(property = "condition=true", service = AggregateState.class)
public class AggregateState {

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
		Reason reason = fsd.waitForStart();
		if (reason == Reason.INTERRUPTED)
			return () -> {};

		tracker.open();
		inited = true;
		System.out.println("tracker open, seen all bundles");
		return () -> tracker.close();
	}

	@Deactivate
	void close() throws Exception {
		synchronized (this) {
			closed = true;
		}
		close(opentracker);
		trackedServices.values()
			.forEach(TrackedService::close);
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
	}

	synchronized AutoCloseable add(Bundle bundle) {
		if (closed)
			return null;

		TrackedBundle trackedBundle = TrackedBundle.create(bundle);
		if (trackedBundle == null)
			return null;
		bundles.add(trackedBundle);

		for (ServiceInfo info : trackedBundle.infos.values()) {
			TrackedService trackedService = trackedServices.computeIfAbsent(info.serviceType,
				st -> new TrackedService(this, st));

			trackedService.add(info);
		}
		return () -> remove(trackedBundle);
	}

	private synchronized void remove(TrackedBundle trackedBundle) {
		if (closed)
			return;

		bundles.remove(trackedBundle);
		for (ServiceInfo info : trackedBundle.infos.values()) {
			TrackedService trackedService = trackedServices.get(info.serviceType);
			if (trackedService.remove(info)) {
				TrackedService removed = trackedServices.remove(trackedService.serviceType);
				removed.close();
			}
		}
	}

	void schedule(Runnable run, long delay) {
		synchronized (this) {
			if (!closed) {
				if (delay == 0)
					executor.execute(run);
				else
					executor.schedule(run, delay, TimeUnit.MILLISECONDS);
				return;
			}
		}
		run.run();
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
