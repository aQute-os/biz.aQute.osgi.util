package biz.aQute.aggregate.provider;

import static biz.aQute.aggregate.api.AggregateConstants.AGGREGATE_OVERRIDE;
import static biz.aQute.aggregate.api.AggregateConstants.AGGREGATE_OVERRIDE_ACTUAL_ATTR;
import static biz.aQute.aggregate.api.AggregateConstants.AGGREGATE_OVERRIDE_PROMISE_ATTR;
import static biz.aQute.aggregate.provider.AggregateState.ARCHETYPE;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import aQute.libg.parameters.Attributes;
import aQute.libg.parameters.ParameterMap;
import biz.aQute.aggregate.api.Aggregate;
import biz.aQute.aggregate.api.AggregateConstants;
import biz.aQute.hlogger.util.HLogger;

/**
 * Parses a bundle's information. By default we look at the osgi.service
 * capabilities. The requirements are translated to the actual types that are an
 * aggregation of a service type.
 * <p>
 * For each service requirement, we load the class and check it is an interface
 * that extends Collection. If so, we extract the service type.
 * <p>
 * For each service capability we count that service is a promise to register 1
 * service. Unfortunately, only one capability is provided even though there are
 * multiple components that provide that service.
 * <p>
 * For this reason, we can override these defaults with an
 * {@value AggregateConstants#AGGREGATE_OVERRIDE} header. This header can
 * override the number of actualTypes and promised types.
 */
@SuppressWarnings("rawtypes")
class TrackedBundle {
	final static String				FILTER_DIRECTIVE	= "filter:";
	final static String				OSGI_SERVICE		= "osgi.service";
	final static Pattern			FILTER_P			= Pattern.compile("\\(objectClass=(?<class>[^)]+)\\)");

	final Bundle					bundle;
	final Map<Class, ServiceInfo>	infos				= new HashMap<>();
	final HLogger					logger;

	class ServiceInfo {
		final Class			serviceType;
		final List<Class>	actualTypes	= new ArrayList<>();

		int					promised;

		ServiceInfo(Class serviceType) {
			this.serviceType = serviceType;
		}

		@Override
		public String toString() {
			return "ServiceInfo [serviceType=" + serviceType.getSimpleName() + ", actualTypes=" + actualTypes
				+ ", promised="
				+ promised + "]";
		}

	}

	public static TrackedBundle create(Bundle bundle) {
		TrackedBundle tb = new TrackedBundle(bundle);
		if (tb.infos.isEmpty())
			return null;
		else
			return tb;

	}

	TrackedBundle(Bundle bundle) {
		this.logger = AggregateState.logger.child("[" + bundle.getBundleId() + "]");
		this.bundle = bundle;
		doRequireCapability();
		doProvideCapability();
		doAggregateHeader();
	}

	void doRequireCapability() {
		String header = bundle.getHeaders()
			.get(Constants.REQUIRE_CAPABILITY);
		ParameterMap parameters = new ParameterMap(header);
		for (Entry<String, Attributes> e : parameters.entrySet()) {
			String clause = "Require-Capability: " + e.toString();

			String namespace = ParameterMap.removeDuplicateMarker(e.getKey());
			if (OSGI_SERVICE.equals(namespace)) {
				String filter = e.getValue()
					.get(FILTER_DIRECTIVE);
				if (filter != null) {
					String actualTypeName = getTypeFromFilter(filter);
					Class actualType = loadClass(bundle, actualTypeName);
					if (actualType != null) {
						if (AggregateState.ARCHETYPE.isAssignableFrom(actualType)) {
							Class serviceType = getServiceType(actualType);
							if (serviceType == null) {
								continue;
							}
							ServiceInfo info = getInfo(serviceType);
							info.actualTypes.add(actualType);
						}
					} else
						logger.info("%s : actual type %s could not be loaded", clause, actualTypeName);
				} else
					logger.debug("%s : no filter", e);
			}
		}
	}

	void doProvideCapability() {
		String provideHeader = bundle.getHeaders()
			.get(Constants.PROVIDE_CAPABILITY);
		ParameterMap parameters = new ParameterMap(provideHeader);
		for (Entry<String, Attributes> e : parameters.entrySet()) {
			String clause = "Provide-Capability: " + e.toString();
			String key = ParameterMap.removeDuplicateMarker(e.getKey());
			Attributes value = e.getValue();

			String namespace = key;
			if (!namespace.equals(OSGI_SERVICE))
				continue;

			String string = value.get(Constants.OBJECTCLASS);
			if (string == null) {
				logger.info("%s : no mandatory objectClass attribute", clause);
				continue;
			}

			String objectClasses[] = string.trim()
				.split("\\s*,\\s*");

			for (String serviceTypeName : objectClasses) {

				Class serviceType = loadClass(bundle, serviceTypeName);

				if (serviceType == null) {
					logger.warn("%s : objectClass %s cannot be loaded", clause, serviceType);
					continue;
				}

				ServiceInfo info = getInfo(serviceType);
				info.promised++;
			}
		}
	}

	void doAggregateHeader() {
		String header = bundle.getHeaders()
			.get(AGGREGATE_OVERRIDE);
		ParameterMap parameters = new ParameterMap(header);
		for (Entry<String, Attributes> e : parameters.entrySet()) {
			String clause = AGGREGATE_OVERRIDE + ": " + e.toString();

			String key = ParameterMap.removeDuplicateMarker(e.getKey());
			Attributes attrs = e.getValue();

			Class<?> serviceType = loadClass(bundle, key);
			if (serviceType == null) {
				logger.error("%s : service type %s cannot be loaded", clause, key);
				continue;
			}

			ServiceInfo sinfo = getInfo(serviceType);

			doPromises(clause, sinfo, attrs.get(AGGREGATE_OVERRIDE_PROMISE_ATTR));
			doActualTypes(clause, sinfo, attrs.get(AGGREGATE_OVERRIDE_ACTUAL_ATTR));
		}
	}

	private void doActualTypes(String clause, ServiceInfo info, String actualTypeNames) {
		if (actualTypeNames == null)
			return;

		String[] actualTypes = actualTypeNames.trim()
			.split("\\s*,\\s*");

		for (String actualTypeName : actualTypes) {
			Class actualType = loadClass(bundle, actualTypeName);
			if (actualType == null) {
				logger.error("%s : cannot load actual type %s", clause, actualTypeName);
				continue;
			}

			if (!ARCHETYPE.isAssignableFrom(actualType)) {
				logger.error("%s : actual type %s is not an %s", clause, actualType.getName(),
					Aggregate.class.getName());
				continue;
			}

			if (!actualType.isInterface()) {
				logger.error("%s : actual type %s is not an interface", clause, actualType.getName());
				continue;
			}


			Class serviceType = getServiceType(actualType);
			if (serviceType == null) {
				logger.error("%s : actual type %s does not (properly) extend Aggregate<S>", clause, actualType);
				continue;
			}

			if (serviceType != info.serviceType) {
				logger.error("%s : actual type %s extends Aggregate<%s> but was placed on service type %s", clause,
					actualType, serviceType, info.serviceType);
				continue;
			}

			info.actualTypes.add(actualType);
		}
	}

	private void doPromises(String clause, ServiceInfo info, String promises) {
		if (promises == null)
			return;

		try {
			int n = Integer.parseInt(promises);
			if (n < 0) {
				logger.error("%s promises must be > 0, it is %s", clause, n);
				n = 0;
			}
			info.promised = n;
		} catch (NumberFormatException e) {
			logger.error("%s promises not a number: %s", clause, e);
			info.promised = 1;
		}

	}

	ServiceInfo getInfo(Class serviceType) {
		return infos.computeIfAbsent(serviceType, ServiceInfo::new);
	}

	private static String getTypeFromFilter(String filter) {
		Matcher matcher = FILTER_P.matcher(filter);
		if (matcher.find()) {
			return matcher.group("class");
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Class getServiceType(Class actualType) {

		for (Type type : actualType.getGenericInterfaces()) {
			if (!(type instanceof ParameterizedType))
				continue;

			ParameterizedType ptype = (ParameterizedType) type;
			Type rawType = ptype.getRawType();
			if (rawType != ARCHETYPE) {
				continue;
			}

			Type serviceType = ptype.getActualTypeArguments()[0];
			if (serviceType instanceof Class)
				return (Class) serviceType;
		}
		return null;
	}

	private static Class<?> loadClass(Bundle bundle, String className) {
		try {
			return bundle.loadClass(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public void close() {}

	@Override
	public String toString() {
		return "TrackedBundle [bundle=" + bundle + ", infos=" + infos + "]";
	}

}
