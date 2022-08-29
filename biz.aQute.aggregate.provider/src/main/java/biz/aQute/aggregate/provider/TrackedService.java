package biz.aQute.aggregate.provider;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import biz.aQute.aggregate.api.Aggregate;

@SuppressWarnings({
	"rawtypes", "unchecked"
})
class TrackedService {

	final Class									serviceType;
	final Set<TrackedBundle>					usedOffers			= new HashSet<>();
	final Map<Class, ActualTypeRegistration>	registrations		= new HashMap<>();
	final ServiceTracker						tracker;
	final AggregateState						state;
	final int									adjust;

	int											discovered			= 0;
	int											requiredByBundles	= 0;

	class ActualTypeRegistration {
		final Class					actualType;
		final Aggregate				annotation;
		final List<TrackedListener>	clients		= new ArrayList<>();
		final AtomicBoolean			changed		= new AtomicBoolean(true);
		final int					localAdjust;
		boolean						satisfied	= false;
		ServiceRegistration			reg;

		ActualTypeRegistration(Class actualType) {
			this.actualType = actualType;
			this.annotation = (Aggregate) actualType.getAnnotation(Aggregate.class);
			this.localAdjust = annotation != null ? annotation.adjust() + adjust : 0;
		}

		private void register() {

			if (reg != null)
				return;

			synchronized (state) {
				if (state.closed)
					return;
			}

			System.out.println("registering " + actualType);
			try {
				Object instance = getInstance(actualType);
				ServiceRegistration reg = state.context.registerService(actualType, instance, null);
				synchronized (state) {
					this.reg = reg;
					if (!state.closed) {
						return;
					}
					// oops, closed in the mean time
				}
				unregister();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void unregister() {
			if (reg == null)
				return;
			reg.unregister();
			reg = null;
			System.out.println("unregistering " + actualType);
		}

		// serialized on scheduler single thread
		void update() {
			assert !state.holdsLock();

			boolean satisfied;
			synchronized (state) {
				if (state.closed)
					return;

				satisfied = isSatisfied();
				if (satisfied == this.satisfied)
					return;
				this.satisfied = satisfied;
			}
			if (satisfied)
				register();
			else
				unregister();

			synchronized (state) {
				if (!state.closed)
					return;
			}
			unregister();
		}

		boolean isSatisfied() {
			return discovered >= requiredByBundles + adjust + localAdjust;
		}

		void refresh() {
			assert state.holdsLock();
			boolean satisfied = isSatisfied();
			if (satisfied == this.satisfied)
				return;

			state.schedule(this::update, satisfied ? 500 : 0);
		}

		private Object getInstance(Class actual) throws InstantiationException, IllegalAccessException {
			Object instance;
			if (actual.isInterface()) {
				instance = Proxy.newProxyInstance(actual.getClassLoader(), new Class[] {
					actual
				}, (p, m, a) -> {
					throw new UnsupportedOperationException(
						"This was registered as an indication of the aggregate state for " + serviceType
							+ ". It does not support any impl.");
				});
			} else
				instance = actual.newInstance();
			return instance;
		}

		public void close() {
			state.schedule(this::unregister, 0);
		}
	}

	TrackedService(AggregateState state, Class serviceType) {
		assert state.holdsLock();
		this.state = state;
		this.serviceType = serviceType;

		this.adjust = Integer.getInteger("aggregate." + serviceType.getName(), 0);
		this.tracker = new ServiceTracker(state.context, this.serviceType, null) {
			@Override
			public Object addingService(ServiceReference reference) {
				synchronized (state) {
					discovered++;
					refresh();
				}
				return reference;
			}

			@Override
			public void removedService(ServiceReference reference, Object service) {
				synchronized (state) {
					discovered--;
					refresh();
				}
			}
		};
		this.state.bundles.forEach(this::add);
		this.tracker.open();
	}

	void add(TrackedListener ts) {
		assert state.holdsLock();
		ActualTypeRegistration atr = registrations.computeIfAbsent(ts.actualType, ActualTypeRegistration::new);
		atr.clients.add(ts);
		atr.refresh();
	}

	boolean remove(TrackedListener ts) {
		assert state.holdsLock();
		ActualTypeRegistration atr = registrations.get(ts.actualType);
		atr.clients.remove(ts);
		if (atr.clients.isEmpty()) {
			registrations.remove(ts.actualType);
			atr.close();
			return registrations.isEmpty();
		} else
			return false;
	}

	void add(TrackedBundle tb) {
		assert state.holdsLock();
		int n = tb.getOfferedServices(serviceType);
		if (n > 0) {
			usedOffers.add(tb);
			requiredByBundles += n;
			refresh();
		}
	}

	void remove(TrackedBundle tb) {
		assert state.holdsLock();
		if (usedOffers.remove(tb)) {
			requiredByBundles -= tb.getOfferedServices(serviceType);
			refresh();
		}
	}

	private void refresh() {
		assert state.holdsLock();
		registrations.values()
			.forEach(r -> r.refresh());
	}

	void close() {
		registrations.values()
			.forEach(ActualTypeRegistration::close);
	}

	@Override
	public String toString() {
		return "TrackedService [serviceType=" + serviceType + ", clients=" + registrations.size() + ", registrations="
			+ registrations.size() + ", discovered=" + discovered + ", required=" + requiredByBundles + "]";
	}

}
