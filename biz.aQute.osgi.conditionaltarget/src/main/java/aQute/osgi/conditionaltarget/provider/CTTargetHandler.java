package aQute.osgi.conditionaltarget.provider;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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

/**
 * This class handles a single target from a bundle. That usually maps to a
 * single @Reference to a ConditionalTarget service.
 *
 * @param <TT>
 *            the type of the ConditionalTarget
 */
class CTTargetHandler<TT> {
	final static Logger																logger	= LoggerFactory
			.getLogger(ConditionalTarget.class);
	final static String[]															SPECIAL	= new String[] { "T", "#" };

	final String																	filter;
	final ServiceTracker<TT, ServiceReference<TT>>									targetTypeTracker;
	final MultiMap<String, BiFunction<Map<String, Object>, List<Object>, Object>>	activeKeys;
	final Class<TT>																	type;
	final BundleContext																context;
	final LockedOperations															locked	= new LockedOperations();

	/*
	 * Gathers the operations and their variables that must be locked per
	 * Conditional Target. Each ConditionalTargetImpl depends on the set of
	 * ServiceReferences tracked. We therefore lock all operations that modify
	 * tracked and/or instances and ensure the ConditionalTargetImpls are always
	 * up to date with the tracked list.
	 */

	class LockedOperations {

		final List<ServiceReference<TT>>			trackedReferences	= new ArrayList<>();
		final List<CTBundleInstance<TT>>			instances			= new ArrayList<>();
		final AtomicInteger							requests			= new AtomicInteger(0);

		Hashtable<String, Object>					lastProperties;
		ServiceRegistration<ConditionalTarget<TT>>	registration;
		boolean										open = true;

		void addService(ServiceReference<TT> reference) {
			try {
				synchronized (this) {
					if (!open)
						return;
					trackedReferences.add(reference);
					instances.forEach(instance -> instance.add(reference));
					lastProperties = aggregate();
				}
				update();
			} catch (Exception e) {
				logger.error("unexpected {} adding {}", e, reference);
			}
		}

		void removeService(ServiceReference<TT> reference) {
			try {
				synchronized (this) {
					if (!open)
						return;
					trackedReferences.remove(reference);
					instances.forEach(instance -> instance.remove(reference));
					lastProperties = aggregate();
				}
				update();
			} catch (Exception e) {
				logger.error("unexpected {} removing {}", e, reference);
			}
		}

		private void update() {
			if (requests.incrementAndGet() != 1) {
				return;
			}

			do {
				registration.setProperties(lastProperties);
			} while (requests.decrementAndGet() != 0 && open);
		}

		synchronized void addInstance(ServiceRegistration<ConditionalTarget<TT>> registration,
				CTBundleInstance<TT> instance) {
			this.registration = registration;
			if (!open)
				return;

			this.instances.add(instance);
			this.trackedReferences.forEach(instance::add);
		}

		synchronized void removeInstance(ConditionalTarget<TT> instance) {
			if (!open)
				return;
			instances.remove(instance);
		}

		synchronized Map<String, Object> properties() {
			if ( open)
				return lastProperties;

			return Collections.emptyMap();
		}

		synchronized int getSize() {
			return trackedReferences.size();
		}

		/*
		 * Notice that this can come later if an instance was added during
		 * registration we therefore also get the registration from the service
		 * factory when a new instance is added.
		 */
		synchronized void registration(ServiceRegistration<ConditionalTarget<TT>> register) {
			this.registration = register;
		}

		/*
		 * Calculate the aggregate properties
		 */
		private Hashtable<String, Object> aggregate() {
			Hashtable<String, Object> serviceProperties = new Hashtable<>();
			if (registration == null)
				return serviceProperties;

			for (Map.Entry<String, List<BiFunction<Map<String, Object>, List<Object>, Object>>> entry : activeKeys
					.entrySet()) {
				String key = entry.getKey();
				List<Object> values = new ArrayList<>();

				if (!Strings.in(SPECIAL, key)) {
					for (ServiceReference<?> ref : trackedReferences) {
						Object value = ref.getProperty(key);
						aggregate(values, value);
					}
				}

				for (BiFunction<Map<String, Object>, List<Object>, Object> f : entry.getValue()) {
					f.apply(serviceProperties, values);
				}
			}
			return serviceProperties;
		}

		private void aggregate(List<Object> l, Object value) {
			if (value == null)
				return;

			Class<?> clazz = value.getClass();
			if (clazz.isArray()) {

				if (clazz.getComponentType().isPrimitive()) {
					if (clazz.getComponentType() == byte.class) {
						l.add(value);
					} else {

						for (int i = 0; i < Array.getLength(value); i++) {
							Object n = Array.get(value, i);
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
			synchronized (this) {
				if (!open)
					return;
				open = false;
				if ( registration == null)
					return;
			}
			registration.unregister();
		}

	}

	/*
	 * Constructor
	 */
	CTTargetHandler(String filter, Class<TT> type, BundleContext context) throws InvalidSyntaxException {
		this.filter = filter;
		this.context = context;
		this.type = type;
		this.activeKeys = calculateKeyHandlers(FilterParser.getAttributes(filter));

		Filter f = createFilter(type);
		logger.info("Tracking services {}", f);
		this.locked.registration(register());
		this.targetTypeTracker = createTracker(f);
		//
		// We track all services since the CTBundleInstance will check them out
		//
		this.targetTypeTracker.open(true);
	}

	/*
	 * Track all service that have one of the properties we're looking for
	 */
	private ServiceTracker<TT, ServiceReference<TT>> createTracker(Filter f) {
		return new ServiceTracker<TT, ServiceReference<TT>>(context, f, null) {

			@Override
			public ServiceReference<TT> addingService(ServiceReference<TT> reference) {
				locked.addService(reference);
				return reference;
			}

			@Override
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
		logger.info("Register Conditional Target for {} and filter {}", type, filter);
		return (ServiceRegistration<ConditionalTarget<TT>>) context
				.registerService(ConditionalTarget.class.getName(), new ServiceFactory<ConditionalTarget<TT>>() {

					@Override
					public ConditionalTarget<TT> getService(Bundle bundle,
							ServiceRegistration<ConditionalTarget<TT>> registration) {
						CTBundleInstance<TT> instance = new CTBundleInstance<>(bundle.getBundleContext(),
								CTTargetHandler.this);

						locked.addInstance(registration, instance);
						return instance;
					}

					@Override
					public void ungetService(Bundle bundle, ServiceRegistration<ConditionalTarget<TT>> registration,
							ConditionalTarget<TT> service) {
						locked.removeInstance(service);
						((CTBundleInstance<?>) service).close();
					}
				}, null);
	}

	/*
	 * Create a filter based on the set of keys
	 */
	private Filter createFilter(Class<?> type) throws InvalidSyntaxException {
		StringBuilder filterBuilder = new StringBuilder();
		filterBuilder.append("(objectClass=").append(type.getName()).append(")");
		Filter f = context.createFilter(filterBuilder.toString());
		return f;
	}

	/*
	 * Every key specified in the target filter requires a handler that can
	 * aggregate the key type. We add a special key handler for each key that is
	 * specified.
	 */

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

	private Object getValue(List<Object> l) {
		if (l.isEmpty())
			return null;
		if (l.size() == 1)
			return l.get(0);

		return l;
	}

	void close() {
		locked.close();
		targetTypeTracker.close();
	}

}