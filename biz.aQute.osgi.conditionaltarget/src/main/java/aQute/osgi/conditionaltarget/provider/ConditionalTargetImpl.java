package aQute.osgi.conditionaltarget.provider;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.collections.MultiMap;
import aQute.lib.strings.Strings;
import aQute.osgi.conditionaltarget.api.ConditionalTarget;

class ConditionalTargetImpl<TT> {
	final static Logger																logger	= LoggerFactory
			.getLogger(ConditionalTarget.class);
	final static String[]															SPECIAL	= new String[] { "T", "#" };
	final ServiceRegistration<ConditionalTarget<TT>>								registration;
	final String																	filter;
	final ServiceTracker<TT, ServiceReference<TT>>									tracker;
	final MultiMap<String, BiFunction<Map<String, Object>, List<Object>, Object>>	activeKeys;
	final Class<TT>																	type;
	final BundleContext																context;
	boolean																			open	= true;

	/*
	 * Gathers the operations and their variables that must be locked per Conditional Target
	 */
	
	class LockedOperations {
		
		final List<ServiceReference<TT>>												tracked	= new ArrayList<>();
		final List<ConditionalTargetProxy<TT>>											proxies	= new ArrayList<>();
		
		synchronized ServiceReference<TT> addService(ServiceReference<TT> reference) {
			try {
				tracked.add(reference);
				aggregate(tracked);
				proxies.forEach(proxy -> proxy.add(reference));
				return reference;
			} catch (Exception e) {
				logger.error("unexpected {} adding {}", e, reference);
				return null;
			}
		}

		synchronized void removeService(ServiceReference<TT> reference) {
			try {
				tracked.remove(reference);
				proxies.forEach(proxy -> proxy.remove(reference));
				aggregate(tracked);
			} catch (Exception e) {
				logger.error("unexpected {} removing {}", e, reference);
			}
		}

		synchronized void addProxy(ConditionalTargetProxy<TT> proxy) {
			proxies.add(proxy);
			tracked.forEach( proxy::add );
		}

		synchronized void removeProxy(ConditionalTarget<TT> proxy) {
			proxies.remove(proxy);
		}

		public Map<String, Object> properties() {
			return aggregate0(tracked);
		}

		public int getSize() {
			return tracked.size();
		}
	}
	final LockedOperations locked = new LockedOperations();

	@SuppressWarnings("unchecked")
	ConditionalTargetImpl(String filter, Class<?> type, BundleContext context) throws InvalidSyntaxException {
		this.filter = filter;
		this.context = context;
		this.type = (Class<TT>) type;
		this.activeKeys = calculateKeyHandlers(FilterParser.getAttributes(filter));
		
		Filter f = createFilter(type, activeKeys.keySet());
		this.tracker = createTracker(f);
		this.registration = register();
		this.tracker.open(true);
	}

	/*
	 * Track all service that have one of the properties we're looking for
	 */
	private ServiceTracker<TT, ServiceReference<TT>> createTracker(Filter f) {
		return new ServiceTracker<TT, ServiceReference<TT>>(context, f, null) {

			public ServiceReference<TT> addingService(ServiceReference<TT> reference) {
				return locked.addService(reference);
			}

			public void removedService(ServiceReference<TT> reference,
					ServiceReference<TT> service) {
				locked.removeService(service);
			}
		};
	}

	/*
	 * Register a Service Factory so we can deliver the services from that
	 * bundle instead of our Bundle Context.
	 */
	@SuppressWarnings("unchecked")
	private ServiceRegistration<ConditionalTarget<TT>> register() {
		return (ServiceRegistration<ConditionalTarget<TT>>) context
				.registerService(ConditionalTarget.class.getName(), new ServiceFactory<ConditionalTarget<TT>>() {

					@Override
					public ConditionalTarget<TT> getService(Bundle bundle,
							ServiceRegistration<ConditionalTarget<TT>> registration) {
						ConditionalTargetProxy<TT> proxy = new ConditionalTargetProxy<TT>(bundle.getBundleContext(),
								ConditionalTargetImpl.this);

						locked.addProxy(proxy);
						return proxy;
					}

					@Override
					public void ungetService(Bundle bundle, ServiceRegistration<ConditionalTarget<TT>> registration,
							ConditionalTarget<TT> service) {
						locked.removeProxy(service);
						((ConditionalTargetProxy<?>) service).close();
					}
				}, null);
	}

	private Filter createFilter(Class<?> type, Set<String> rootKeys) throws InvalidSyntaxException {
		StringBuilder filterBuilder = new StringBuilder();
		int n = 0;
		filterBuilder.append("(objectClass=").append(type.getName()).append(")");
		if (n != 1) {
			filterBuilder.insert(0, "(|").append(")");
		}
		Filter f = context.createFilter(filterBuilder.toString());
		logger.info("filter {}", f);
		return f;
	}

	private MultiMap<String, BiFunction<Map<String, Object>, List<Object>, Object>> calculateKeyHandlers(
			Set<String> allKeys) {
		MultiMap<String, BiFunction<Map<String, Object>, List<Object>, Object>> activeKeys = new MultiMap<>();
		activeKeys.add("T", (map, l) -> map.put("T", type.getName()));

		for (String key : allKeys) {

			switch (key) {
			case "#":
				activeKeys.add(key, (map, l) -> map.put(key, locked.getSize()));
				break;

			default:
				for (KeyOption kOption : KeyOption.values()) {
					if (key.startsWith(kOption.symbol)) {
						String rootKey = key.substring(kOption.symbol.length());
						activeKeys.add(rootKey, (map, l) -> map.put(key, kOption.value(l)));
						continue; // a key can match only one option
					}
				}
				// only when it is not an option
				activeKeys.add(key, (map, l) -> {
					Object value = getValue(l);
					if (value != null)
						return map.put(key, value);
					else
						return null;
				});
			}
		}
		return activeKeys;
	}

	private void aggregate(List<ServiceReference<TT>> tracked) {
		if (!open)
			return;

		registration.setProperties(aggregate0(tracked));

	}

	Hashtable<String, Object> aggregate0(List<ServiceReference<TT>> tracked) {
		Hashtable<String, Object> serviceProperties = new Hashtable<>();
		if (!open)
			return serviceProperties;

		for (Map.Entry<String, List<BiFunction<Map<String, Object>, List<Object>, Object>>> entry : activeKeys
				.entrySet()) {
			String key = entry.getKey();
			List<Object> values = new ArrayList<>();

			if (!Strings.in(SPECIAL, key)) {
				for (ServiceReference<?> ref : tracked) {
					Object value = ref.getProperty(key);
					aggregate(values, value);
				}
			}

			for (BiFunction<Map<String, Object>, List<Object>, Object> f : entry.getValue()) {
				f.apply(serviceProperties, values);
			}
		}
		System.out.println("properties " + serviceProperties);
		return serviceProperties;
	}

	private Object getValue(List<Object> l) {
		if (l.isEmpty())
			return null;
		if (l.size() == 1)
			return l.get(0);

		return l;
	}

	private void aggregate(List<Object> l, Object value) {
		if (value == null)
			return;

		Class<?> clazz = value.getClass();
		if (clazz.isArray()) {

			if (clazz.getComponentType().isPrimitive()) {
				if (clazz.getComponentType() == boolean.class || clazz.getComponentType() == char.class
						|| clazz.getComponentType() == byte.class) {
					l.add(value);
				} else {
					// numbers
					for (int i = 0; i < Array.getLength(value); i++) {
						Number n = (Number) Array.get(value, i);
						l.add(n);
					}
				}
			} else {
				Object[] array = (Object[]) value;
				l.addAll(Arrays.asList(array));
			}
		} else if (Collection.class.isAssignableFrom(clazz)) {
			Collection<?> c = (Collection<?>) value;
			l.addAll(c);
		} else {
			l.add(value);
		}
	}

	void close() {
		synchronized (locked) {
			open = false;
			tracker.close();
			registration.unregister();
		}
	}

}