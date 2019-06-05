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
 * This is the actual service handed out by the {@link ConditionalTargetImpl} via Service Factory
 *
 * @param <TT> the type
 */
class ConditionalTargetProxy<TT> implements ConditionalTarget<TT> {

	final ConditionalTargetImpl<TT>		impl;
	final Map<ServiceReference<TT>, TT>	references	= new HashMap<>();
	final BundleContext					context;

	ConditionalTargetProxy(BundleContext context, ConditionalTargetImpl<TT> impl) {
		this.context = context;
		this.impl = impl;
	}

	@Override
	public List<TT> getServices() {
		synchronized (references) {
			return new ArrayList<>(references.values());
		}
	}

	@Override
	public Map<ServiceReference<TT>, TT> getServiceReferences() {
		synchronized (references) {
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

	public void close() {
		synchronized (references) {
			references.keySet().forEach(context::ungetService);
		}
	}

	void remove(ServiceReference<TT> ref) {
		synchronized (references) {
			references.remove(ref);
			context.ungetService(ref);
		}
	}

	void add(ServiceReference<TT> ref) {
		synchronized (references) {
			TT service = context.getService(ref);
			references.put(ref, service);
		}
	}

}
