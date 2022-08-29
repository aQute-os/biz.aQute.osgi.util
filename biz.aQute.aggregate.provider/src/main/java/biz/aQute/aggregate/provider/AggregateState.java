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
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;

@SuppressWarnings({
	"rawtypes", "unchecked"
})
class AggregateState implements AutoCloseable {

	final BundleContext					context;
	final Map<Class, TrackedService>	trackedServices	= new HashMap<Class, TrackedService>();
	final List<TrackedBundle>			bundles			= new CopyOnWriteArrayList<>();
	final ScheduledExecutorService		executor		= Executors.newSingleThreadScheduledExecutor();

	volatile boolean					closed			= false;

	AggregateState(BundleContext context) {
		this.context = context;
	}

	@Override
	public void close() throws Exception {
		synchronized (this) {
			if (closed)
				return;
			closed = true;
		}
		trackedServices.values()
			.forEach(TrackedService::close);
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
	}

	synchronized AutoCloseable add(Bundle bundle) {
		TrackedBundle trackedBundle = TrackedBundle.create(bundle);
		if (trackedBundle == null)
			return null;
		bundles.add(trackedBundle);
		trackedServices.values()
			.forEach(ts -> ts.add(trackedBundle));
		return () -> remove(trackedBundle);
	}

	private synchronized void remove(TrackedBundle tb) {
		bundles.remove(tb);
		trackedServices.values()
			.forEach(ts -> ts.remove(tb));
	}

	synchronized AutoCloseable add(ListenerInfo li) {
		TrackedListener tl = TrackedListener.create(li);
		if (tl == null)
			return null;

		TrackedService trackedService = trackedServices.computeIfAbsent(tl.serviceType,
			x -> new TrackedService(this, x));
		trackedService.add(tl);
		return () -> {
			synchronized (this) {
				if (trackedService.remove(tl)) {
					trackedServices.remove(trackedService.serviceType);
				}

			}
		};
	}

	synchronized void schedule(Runnable run, long delay) {
		if (delay == 0)
			executor.execute(run);
		else
			executor.schedule(run, delay, TimeUnit.MILLISECONDS);
	}

	public boolean holdsLock() {
		return Thread.holdsLock(this);
	}
}
