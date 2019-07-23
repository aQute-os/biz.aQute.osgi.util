package aQute.osgi.conditionaltarget.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import aQute.osgi.conditionaltarget.api.ConditionalTarget;

/**
 * This is the actual service handed out by the {@link CTTargetHandler} via
 * Service Factory. It is updated from the {@link CTTargetHandler} with the
 * found references. It checks out the actual services and manages their life cycle.
 *
 * @param <TT>
 *            the type
 */
class CTBundleInstance<TT> implements ConditionalTarget<TT> {

	final CTTargetHandler<TT>			impl;
	final Map<ServiceReference<TT>, TT>	references	= new HashMap<>();
	final BundleContext					context;
	final Object						lock		= new Object();

	CTBundleInstance(BundleContext context, CTTargetHandler<TT> impl) {
		this.context = context;
		this.impl = impl;
	}

	@Override
	public List<TT> getServices() {
		synchronized (lock) {
			return new ArrayList<>(references.values());
		}
	}

	@Override
	public Map<ServiceReference<TT>, TT> getServiceReferences() {
		synchronized (lock) {
			return new HashMap<>(references);
		}
	}

	@Override
	public Map<String, Object> getAggregateProperties() {
		return impl.locked.properties();
	}

	@Override
	public Iterator<TT> iterator() {
		return getServices().iterator();
	}

	void remove(ServiceReference<TT> ref) {
		synchronized (lock) {
			references.remove(ref);
		}
		context.ungetService(ref);
	}

	void add(ServiceReference<TT> ref) {
		TT service = context.getService(ref);
		synchronized (lock) {
			if (service != null) {
				references.put(ref, service);
			} else {
				CTTargetHandler.logger.info("get service returned null : {}",ref);
			}
		}
	}

	void close() {
		List<ServiceReference<TT>> refs = new ArrayList<>();
		synchronized (lock) {
			refs.addAll(references.keySet());
			references.clear();
		}
		refs.forEach( context::ungetService );
	}

}
