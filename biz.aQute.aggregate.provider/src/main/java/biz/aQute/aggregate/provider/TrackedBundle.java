package biz.aQute.aggregate.provider;

import static biz.aQute.aggregate.api.AggregateConstants.AGGREGATE_OVERRIDE;
import static biz.aQute.aggregate.api.AggregateConstants.AGGREGATE_OVERRIDE_PROMISE_ATTR;

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
import biz.aQute.aggregate.api.AggregateConstants;

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
	private static final String		FILTER_DIRECTIVE	= "filter:";
	private static final String		OSGI_SERVICE		= "osgi.service";
	final static Pattern			FILTER_P			= Pattern.compile("\\(objectClass=(?<class>[^)]+)\\)");
	final Bundle					bundle;
	final Map<Class, ServiceInfo>	infos				= new HashMap<>();

	class ServiceInfo {
		final Class			serviceType;
		final List<Class>	actualTypes	= new ArrayList<>();
		int					promised;

		ServiceInfo(Class serviceType) {
			this.serviceType = serviceType;
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
		this.bundle = bundle;
		doRequireCapability();
		doProvideCapability();
		doAggregateHeader();
	}

	void doRequireCapability() {
		String header = bundle.getHeaders()
			.get(Constants.REQUIRE_CAPABILITY);
		if (header != null && !header.isEmpty()) {
			ParameterMap parameters = new ParameterMap(header);
			for (Entry<String, Attributes> e : parameters.entrySet()) {
				String namespace = ParameterMap.removeDuplicateMarker(e.getKey());
				if (OSGI_SERVICE.equals(namespace)) {
					String filter = e.getValue()
						.get(FILTER_DIRECTIVE);
					if (filter != null) {
						String actualTypeName = getTypeFromFilter(filter);
						Class actualType = loadClass(bundle, actualTypeName);
						if (actualType != null) {
							Class serviceType = getServiceType(actualType);
							if (serviceType != null) {
								ServiceInfo info = getInfo(serviceType);
								info.actualTypes.add(actualType);
								System.out.println("tracking " + serviceType + " for " + actualType);
							}
						}
					}
				}
			}
		}

	}

	void doProvideCapability() {
		String provideHeader = bundle.getHeaders()
			.get(Constants.PROVIDE_CAPABILITY);
		ParameterMap parameters = new ParameterMap(provideHeader);
		for (Entry<String, Attributes> e : parameters.entrySet()) {
			String namespace = ParameterMap.removeDuplicateMarker(e.getKey());
			if (!namespace.equals(OSGI_SERVICE))
				continue;

			try {
				String string = e.getValue()
					.get(Constants.OBJECTCLASS);
				if (string != null) {
					String objectClasses[] = string.trim()
						.split("\\s*,\\s*");
					for (String serviceTypeName : objectClasses) {
						Class serviceType = loadClass(bundle, serviceTypeName);
						if (serviceType != null) {
							ServiceInfo info = getInfo(serviceType);
							info.promised++;
						}
					}
				}
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}
	}

	void doAggregateHeader() {
		String header = bundle.getHeaders()
			.get(AGGREGATE_OVERRIDE);
		if (header != null && !header.isEmpty()) {
			ParameterMap parameters = new ParameterMap(header);
			for (Entry<String, Attributes> e : parameters.entrySet()) {
				Attributes attrs = e.getValue();

				doPromises(e.getKey(), attrs.get(AGGREGATE_OVERRIDE_PROMISE_ATTR));
				doActualTypes(attrs.get(AggregateConstants.AGGREGATE_OVERRIDE_ACTUAL_ATTR));
			}
		}
	}

	private void doActualTypes(String demand) {
		String actualTypeNames = demand;
		if (actualTypeNames != null) {
			String[] actualTypes = actualTypeNames.trim()
				.split("\\s*,\\s*");

			for (String actualTypeName : actualTypes) {
				Class actualType = loadClass(bundle, actualTypeName);
				if (actualType != null) {
					Class serviceType = getServiceType(actualType);
					if (serviceType != null) {
						ServiceInfo info = getInfo(serviceType);
						info.actualTypes.add(actualType);
					}
				}
			}
		}
	}

	private void doPromises(String key, String supply) {
		if (supply == null)
			return;

		String serviceTypeName = ParameterMap.removeDuplicateMarker(key);
		Class<?> serviceType = loadClass(bundle, serviceTypeName);
		if (serviceType != null) {

			int n = Integer.parseInt(supply);
			ServiceInfo info = getInfo(serviceType);
			info.promised = n;

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
	private static Class getServiceType(Class actualType) {
		if (!Iterable.class.isAssignableFrom(actualType)) {
			return null;
		}

		for (Type type : actualType.getGenericInterfaces()) {
			if (!(type instanceof ParameterizedType)) {
				continue;
			}
			ParameterizedType ptype = (ParameterizedType) type;
			Type rawType = ptype.getRawType();
			if (rawType != Iterable.class) {
				continue;
			}

			return (Class) ptype.getActualTypeArguments()[0];
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
