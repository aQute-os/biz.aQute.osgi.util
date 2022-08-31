package biz.aQute.aggregate.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import biz.aQute.aggregate.api.Aggregate;
import biz.aQute.aggregate.api.AggregateConstants;
import biz.aQute.aggregate.provider.TrackedBundle.ServiceInfo;

@SuppressWarnings({
	"rawtypes", "unchecked"
})
class TrackedService {

	final AggregateState						state;
	final Class									serviceType;
	final Map<Class, ActualTypeRegistration>	actualTypes	= new HashMap<>();
	final ServiceTracker						tracker;
	final int									adjust;
	final int									override;

	int											discovered	= 0;
	int											promised	= 0;

	class ActualTypeRegistration {
		final Class			actualType;
		final Aggregate		annotation;
		final AtomicBoolean	changed		= new AtomicBoolean(true);
		final int			localAdjust;
		final int			localOverride;
		boolean				satisfied	= false;
		int					clients		= 0;

		ActualTypeFactory	reg;

		ActualTypeRegistration(Class actualType) {
			this.actualType = actualType;
			this.annotation = (Aggregate) actualType.getAnnotation(Aggregate.class);

			int localAdjust = annotation != null ? annotation.adjust() : 0;
			int localOverride = annotation != null ? annotation.override() : -1;
			this.localAdjust = Integer.getInteger(AggregateConstants.PREFIX_TO_ADJUST + actualType.getName(),
				localAdjust);
			this.localOverride = Integer.getInteger(AggregateConstants.PREFIX_TO_OVERRIDE + actualType.getName(),
				localOverride);
		}

		private void register() {

			synchronized (state) {
				if (state.closed)
					return;
				if (reg != null)
					return;
			}

			System.out.println("registering " + actualType);
			try {
				ActualTypeFactory instance = new ActualTypeFactory(this, state, serviceType);
				ServiceRegistration reg = state.context.registerService(actualType.getName(), instance, null);
				synchronized (state) {
					if (!state.closed) {
						instance.reg = reg;
						this.reg = instance;
						return;
					}
					System.out.println("oops, got closed in the mean time");
				}
				instance.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public String toString() {
			return "ActualTypeRegistration[actualType=" + actualType.getSimpleName() + ", localAdjust=" + localAdjust
				+ ", satisfied=" + satisfied + ", clients=" + clients + ", reg=" + (reg != null) + "]";
		}

		private void unregister() {
			ActualTypeFactory reg;
			synchronized (state) {
				if (this.reg == null)
					return;

				reg = this.reg;
				this.reg = null;
			}
			System.out.println("unregistering " + actualType);
			reg.close();
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
			if (localOverride >= 0)
				return discovered >= localOverride;

			if (override >= 0)
				return discovered >= override;

			return discovered >= promised + adjust + localAdjust;
		}

		void refresh() {
			assert state.holdsLock();
			if (state.closed)
				return;
			boolean satisfied = isSatisfied();
			if (satisfied == this.satisfied)
				return;

			state.schedule(this::update, satisfied ? 500 : 0);
		}

		public void close() {
			ActualTypeFactory reg;
			synchronized (state) {
				if (this.reg != null) {
					reg = this.reg;
					this.reg = null;
				} else
					return;
			}
			reg.close();
		}

		AggregateState state() {
			return state;
		}
	}

	TrackedService(AggregateState state, Class serviceType) {
		assert state.holdsLock();
		this.state = state;
		this.serviceType = serviceType;

		this.override = Integer.getInteger(AggregateConstants.PREFIX_TO_OVERRIDE + serviceType.getName(), -1);
		this.adjust = Integer.getInteger(AggregateConstants.PREFIX_TO_ADJUST + serviceType.getName(), 0);
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
		this.tracker.open();
	}

	void add(ServiceInfo info) {
		assert state.holdsLock();
		boolean refresh = false;
		int n = info.promised;
		if (n > 0) {
			promised += n;
			refresh = true;
		}

		for (Class actualType : info.actualTypes) {
			ActualTypeRegistration atr = actualTypes.computeIfAbsent(actualType, ActualTypeRegistration::new);
			atr.clients++;
			refresh = true;
		}

		if (refresh) {
			refresh();
		}
	}

	boolean remove(ServiceInfo info) {
		assert state.holdsLock();

		for (Class actualType : info.actualTypes) {
			ActualTypeRegistration atr = actualTypes.get(actualType);
			atr.clients--;
			if (atr.clients == 0) {
				actualTypes.remove(actualType);
				atr.close();
			}

		}
		if (info.promised != 0) {
			this.promised -= info.promised;
			refresh();
		}

		return actualTypes.isEmpty() && this.promised == 0;
	}

	private void refresh() {
		assert state.holdsLock();
		actualTypes.values()
			.forEach(r -> r.refresh());
	}

	void close() {
		actualTypes.values()
			.forEach(ActualTypeRegistration::close);
	}

	@Override
	public String toString() {
		return "TrackedService [serviceType=" + serviceType.getName() + ", actual=" + actualTypes.values()
			+ ", discovered=" + discovered + ", promised=" + promised + ", adjust=" + adjust + "]";
	}

}
