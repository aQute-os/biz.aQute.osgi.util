package biz.aQute.aggregate.provider;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import biz.aQute.aggregate.api.Aggregate;
import biz.aQute.aggregate.provider.TrackedService.ActualType;

/**
 * A Service Factory that will lazily create a ServiceTracker to track the
 * services. The service tracker will use the Bundle Context from the calling
 * bundle. This class should be closed.
 */
@SuppressWarnings({
	"rawtypes", "unchecked"
})
class ActualTypeFactory implements ServiceFactory {
	final ActualType					atr;
	final Map<Bundle, ServiceTracker>	trackers		= new HashMap<>();
	final Class							serviceType;
	final AggregateState				state;
	ServiceRegistration					reg;
	boolean								closed;
	Method								GET_SERVICES	= null;

	ActualTypeFactory(ActualType registration, AggregateState state, Class serviceType) {
		this.atr = registration;
		this.state = state;
		this.serviceType = serviceType;
	}

	@Override
	public Object getService(Bundle bundle, ServiceRegistration registration) {
		synchronized (state) {
			if (state.closed)
				return null;
		}
		Object instance = Proxy.newProxyInstance(atr.actualType.getClassLoader(), new Class[] {
			atr.actualType
		}, (p, m, a) -> {
			if (m.getDeclaringClass() == Object.class) {
				return m.invoke(ActualTypeFactory.this, a);
			}
			if (m == GET_SERVICES || isGetServices(m)) {
				ServiceTracker tracker = getTracked(bundle);
				List values;
				if (tracker == null)
					values = Collections.emptyList();
				else
					values = new ArrayList(tracker.getTracked()
						.values());
				return values;
			}
			atr.logger.error("invalid invocation for method %s", m);
			throw new UnsupportedOperationException("This was registered as an indication of the aggregate state for "
				+ serviceType + ". It does not support any impl.");
		});
		return instance;
	}

	private synchronized boolean isGetServices(Method m) {
		if (m.getDeclaringClass() == Aggregate.class && m.getName()
			.equals("getServices") && m.getReturnType() == List.class && m.getParameterCount() == 0) {
			GET_SERVICES = m;
			return true;
		}
		return false;
	}

	private ServiceTracker getTracked(Bundle bundle) {
		ServiceTracker tracker;
		synchronized (this) {
			if (closed)
				return null;
			tracker = trackers.get(bundle);
			if (tracker != null)
				return tracker;

			tracker = new ServiceTracker(bundle.getBundleContext(), serviceType, null);
			trackers.put(bundle, tracker);
		}
		atr.logger.debug("opening tracker for bundle [%s]", bundle.getBundleId());
		tracker.open();
		return tracker;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		ServiceTracker remove;
		synchronized (this) {
			remove = trackers.remove(bundle);
			if (remove == null)
				return;
		}
		atr.logger.debug("closing tracker for bundle [%s]", bundle);
		remove.close();
	}

	public void close() {
		synchronized (this) {
			if (closed)
				return;
			closed = true;
		}
		atr.logger.debug("closing service factory");
		reg.unregister();
		assert trackers.isEmpty();
	}

}
