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
 * This is the actual service handed out by the {@link ReferenceHandler} via Service
 * Factory
 *
 * @param <TT>
 *            the type
 */
class ConditionalTargetImpl<TT> implements ConditionalTarget<TT> {

	final ReferenceHandler<TT>				impl;
	final Map<ServiceReference<TT>, TT>	references	= new HashMap<>();
	final BundleContext					context;
	final Object						lock		= new Object();

	ConditionalTargetImpl(BundleContext context, ReferenceHandler<TT> impl) {
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

	public void close() {
		synchronized (lock) {
			references.keySet().forEach(context::ungetService);
		}
	}

	void remove(ServiceReference<TT> ref) {
		synchronized (lock) {
			references.remove(ref);
			context.ungetService(ref);
		}
	}

	void add(ServiceReference<TT> ref) {
		synchronized (lock) {
			TT service = context.getService(ref);
			references.put(ref, service);
		}
	}

}
