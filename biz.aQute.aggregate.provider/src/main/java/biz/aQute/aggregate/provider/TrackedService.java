package biz.aQute.aggregate.provider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import biz.aQute.aggregate.api.AggregateConstants;
import biz.aQute.aggregate.api.AggregateSettings;
import biz.aQute.aggregate.provider.TrackedBundle.ServiceInfo;
import biz.aQute.hlogger.util.HLogger;

@SuppressWarnings({
	"rawtypes", "unchecked"
})
class TrackedService {

	final AggregateState									state;
	final HLogger											logger;
	final Map<Class, ActualType>							actualTypes			= new HashMap<>();
	final Map<Bundle, BundleInfo>							bundleInfos			= new HashMap<>();
	final Class												serviceType;
	final ServiceTracker<Object, ServiceReference<Object>>	tracker;
	final int												override;

	int														registeredServices	= 0;

	class BundleInfo {
		final Bundle				bundle;
		final Set<ServiceReference>	registered				= new HashSet<>();

		boolean						staticallyDiscovered	= false;
		int							max						= 0;
		int							actual					= 0;
		long						time;

		BundleInfo(Bundle bundle) {
			this.bundle = bundle;
		}

		void registered(ServiceReference reference) {
			time = System.nanoTime();
			boolean added = registered.add(reference);

			assert added : "must not have been present yet";

			actual++;
			if (actual > max) {
				max = actual;
			}
		}

		void unregistered(ServiceReference reference) {
			time = System.nanoTime();
			boolean removed = registered.remove(reference);

			assert removed : "must be present";

			actual--;
		}

		boolean isSatisfied() {
			return !staticallyDiscovered || actual >= max;
		}

		void close() {}

		@Override
		public String toString() {
			return "BundleInfo [bundle=" + bundle + ", registered=" + registered + ", staticallyDiscovered="
				+ staticallyDiscovered + ", max=" + max + ", actual=" + actual + ", time="
				+ TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - time) + "]";
		}

	}

	class ActualType {
		final HLogger			logger;
		final Class				actualType;
		final AggregateSettings	annotation;
		final int				localOverride;
		boolean					satisfied	= false;
		int						clients		= 0;
		ActualTypeFactory		reg;

		ActualType(Class actualType) {
			assert AggregateState.ARCHETYPE.isAssignableFrom(actualType);
			this.logger = TrackedService.this.logger.child(actualType.getSimpleName());
			this.actualType = actualType;
			this.annotation = (AggregateSettings) actualType.getAnnotation(AggregateSettings.class);

			int localOverride = annotation != null ? annotation.override() : -1;
			this.localOverride = Integer.getInteger(AggregateConstants.PREFIX_TO_OVERRIDE + actualType.getName(),
				localOverride);
		}

		private void register() {

			synchronized (state) {
				if (state.closed)
					return;
				if (reg != null) {
					logger.debug("already registered");
					return;
				}
			}

			logger.debug("registering %s", actualType);
			try {
				ActualTypeFactory instance = new ActualTypeFactory(this, state, serviceType);
				Bundle bundle = FrameworkUtil.getBundle(actualType);
				BundleContext context = bundle == null ? state.context : bundle.getBundleContext();
				ServiceRegistration reg = context.registerService(actualType.getName(), instance, null);
				synchronized (state) {
					instance.reg = reg;
					if (!state.closed) {
						this.reg = instance;
						return;
					}
				}
				logger.info("emergency unregister %s", actualType.getName());
				instance.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public String toString() {
			return "ActualTypeRegistration[actualType=" + actualType.getSimpleName() + ", satisfied=" + satisfied
				+ ", clients=" + clients + ", reg=" + (reg != null) + "]";
		}

		private void unregister() {
			ActualTypeFactory reg;
			synchronized (state) {
				if (this.reg == null) {
					logger.debug("already unregistered");
					return;
				}

				reg = this.reg;
				this.reg = null;
			}
			logger.debug("unregistering %s", actualType.getName());
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
				return registeredServices >= localOverride;

			if (override >= 0)
				return registeredServices >= override;

			for (BundleInfo info : bundleInfos.values()) {
				if (!info.isSatisfied())
					return false;
			}
			return true;
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
		this.logger = AggregateState.logger.child("S=" + serviceType.getSimpleName());
		this.state = state;
		this.serviceType = serviceType;

		this.override = Integer.getInteger(AggregateConstants.PREFIX_TO_OVERRIDE + serviceType.getName(), -1);
		this.tracker = new ServiceTracker<Object, ServiceReference<Object>>(state.context, this.serviceType, null) {
			@Override
			public ServiceReference<Object> addingService(ServiceReference<Object> reference) {
				synchronized (state) {
					Bundle bundle = reference.getBundle();
					logger.debug("registered by [%s]", bundle.getBundleId());
					registeredServices++;
					BundleInfo binfo = bundleInfos.computeIfAbsent(bundle, b -> new BundleInfo(b));
					binfo.registered(reference);
					refresh();
				}
				return reference;
			}

			@Override
			public void removedService(ServiceReference<Object> reference, ServiceReference<Object> service) {
				synchronized (state) {
					Bundle bundle = reference.getBundle();
					logger.debug("unregistered by [%s]", bundle.getBundleId());
					registeredServices--;
					BundleInfo binfo = bundleInfos.get(bundle);
					if (binfo != null) {
						binfo.unregistered(reference);
						if (binfo.actual == 0 && !binfo.staticallyDiscovered)
							bundleInfos.remove(bundle);
					}
					refresh();
				}
			}
		};
	}

	void add(Bundle bundle, ServiceInfo info) {
		assert state.holdsLock();

		if (info.promised == 0 && info.actualTypes.isEmpty())
			return;

		if (info.promised > 0) {
			logger.debug("statically detected %s", info);
			BundleInfo binfo = bundleInfos.computeIfAbsent(bundle, b -> new BundleInfo(b));
			binfo.staticallyDiscovered = true;
			binfo.max = Math.max(binfo.max, info.promised);
		}

		for (Class actualType : info.actualTypes) {
			ActualType atr = actualTypes.computeIfAbsent(actualType, ActualType::new);
			atr.clients++;
		}

		if (tracker.getTrackingCount() < 0 && !actualTypes.isEmpty()) {
			logger.debug("opening tracker");
			tracker.open();
		}
		refresh();
	}

	boolean remove(Bundle bundle, ServiceInfo info) {
		assert state.holdsLock();

		logger.debug("remove %s %s", bundle, info);
		for (Class actualType : info.actualTypes) {
			ActualType atr = actualTypes.get(actualType);
			atr.clients--;
			if (atr.clients == 0) {
				logger.debug("no more clients for %s", actualType);
				actualTypes.remove(actualType);
				atr.close();
			}

		}

		BundleInfo removed = bundleInfos.remove(bundle);
		if (removed != null) {
			removed.close();
		}

		refresh();

		return actualTypes.isEmpty() && this.bundleInfos.isEmpty();
	}

	private void refresh() {
		assert state.holdsLock();
		actualTypes.values()
			.forEach(r -> r.refresh());
	}

	void close() {
		if (tracker.getTrackingCount() < 0) {
			logger.debug("closing but never used");
		} else
			logger.debug("closing");
		actualTypes.values()
			.forEach(ActualType::close);
	}

	@Override
	public String toString() {
		return "TrackedService [serviceType=" + serviceType.getName() + ", actual=" + actualTypes.values()
			+ ", bundleInfos=" + bundleInfos.values() + "]";
	}

	public List<BundleInfo> satisfied() {
		synchronized (state) {
			return bundleInfos.values()
				.stream()
				.filter(BundleInfo::isSatisfied)
				.collect(Collectors.toList());
		}
	}

}
