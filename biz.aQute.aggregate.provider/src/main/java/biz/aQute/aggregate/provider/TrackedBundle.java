package biz.aQute.aggregate.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import aQute.libg.parameters.Attributes;
import aQute.libg.parameters.ParameterMap;

@SuppressWarnings("rawtypes")
public class TrackedBundle {
	final Bundle				bundle;
	final Map<Class, Integer>	offers;

	public static TrackedBundle create(Bundle bundle) {
		final Map<Class, Integer> offers = new HashMap<>();

		String provideHeader = bundle.getHeaders().get(Constants.PROVIDE_CAPABILITY);
		if (provideHeader != null && !provideHeader.isEmpty()) {
			ParameterMap parameters = new ParameterMap(provideHeader);
			for (Entry<String, Attributes> e : parameters.entrySet()) {
				String namespace = ParameterMap.removeDuplicateMarker(e.getKey());
				if (!namespace.equals("osgi.service"))
					continue;

				try {
					String string = e.getValue().get(Constants.OBJECTCLASS);
					if (string != null) {
						String objectClasses[] = string.trim().split("\\s*,\\s*");
						for (String className : objectClasses) {
							Class clazz = loadClass(bundle, className);
							if (clazz != null) {
								int os = offers.computeIfAbsent(clazz, c -> 0);
								os++;
								offers.put(clazz, os);
							}
						}
					}
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			}
		}
		String header = bundle.getHeaders().get("Aggregate-Cardinality");
		if (header != null && !header.isEmpty()) {
			ParameterMap parameters = new ParameterMap(header);
			for (Entry<String, Attributes> e : parameters.entrySet()) {
				String className = ParameterMap.removeDuplicateMarker(e.getKey());
				Class<?> clazz = loadClass(bundle, className);
				if (clazz != null) {
					Attributes attrs = e.getValue();
					String cardinality = attrs.getOrDefault("cardinality", "1");
					int n = Integer.parseInt(cardinality);

					// overwrite any calculated by osgi.service capabilities
					offers.put(clazz, n);
				}
			}
		}
		if (offers.isEmpty())
			return null;

		return new TrackedBundle(bundle, offers);
	}

	TrackedBundle(Bundle bundle, Map<Class, Integer> offers) {
		this.bundle = bundle;
		this.offers = offers;
	}

	private static Class<?> loadClass(Bundle bundle, String className) {
		try {
			return bundle.loadClass(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public void close() {
	}

	int getOfferedServices(Class serviceType) {
		return offers.getOrDefault(serviceType, 0);
	}

	@Override
	public String toString() {
		return "TrackedBundle [bundle=" + bundle + ", offers=" + offers + "]";
	}

}
