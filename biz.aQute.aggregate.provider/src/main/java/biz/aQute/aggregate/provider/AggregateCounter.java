package biz.aQute.aggregate.provider;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.BundleTracker;

@Component(property = "condition=true", service = AggregateCounter.class)
public class AggregateCounter {

	final BundleContext						context;
	final BundleTracker<AutoCloseable>		tracker;
	final ServiceRegistration<ListenerHook>	registration;
	final AggregateState					state;
	boolean									closed;

	@Activate
	public AggregateCounter(BundleContext context) {
		this.context = context;
		this.state = new AggregateState(context);
		this.tracker = new BundleTracker<AutoCloseable>(context, Bundle.ACTIVE, null) {
			@Override
			public AutoCloseable addingBundle(Bundle bundle, BundleEvent event) {
				return state.add(bundle);
			}

			@Override
			public void removedBundle(Bundle bundle, BundleEvent event, AutoCloseable tb) {
				AggregateCounter.close(tb);
			}

		};
		this.tracker.open();

		registration = context.registerService(ListenerHook.class, new ListenerHook() {
			final Map<ListenerInfo, AutoCloseable> listeners = new ConcurrentHashMap<>();
			
			@Override
			public void added(Collection<ListenerInfo> listeners) {
				listeners.forEach(l -> {
					AutoCloseable ac = state.add(l);
					if ( ac != null)
						this.listeners.put(l,ac);
				});

			}

			@Override
			public void removed(Collection<ListenerInfo> listeners) {
				listeners.forEach(l -> {
					AutoCloseable remove = this.listeners.remove(l);
					close(remove);
				});
			}

		}, null);
	}

	@Deactivate
	void close() throws Exception {
		closed = true;
		registration.unregister();
		close(state);
		tracker.close();
	}

	static void close(AutoCloseable tb) {
		try {
			if ( tb != null)
				tb.close();
		} catch( Exception e) {
			// ignore
		}
		
	}
}
