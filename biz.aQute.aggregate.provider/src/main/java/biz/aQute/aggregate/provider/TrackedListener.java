package biz.aQute.aggregate.provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;

import biz.aQute.aggregate.api.Aggregate;


@SuppressWarnings("rawtypes")
public class TrackedListener {
	static Pattern FILTER_P = Pattern.compile("\\(objectClass=(?<class>[^)]+)\\)");

	public static TrackedListener create(ListenerInfo l) {
		try {
			String filter = l.getFilter();
			if (filter == null)
				return null;

			Matcher matcher = FILTER_P.matcher(filter);
			if (matcher.find()) {
				String actualTypeName = matcher.group("class");

				Class actualType = l.getBundleContext().getBundle().loadClass(actualTypeName);
				
				Class serviceType = getServiceType(actualType);
				if ( serviceType == null)
					return null;
				
				System.out.println("tracking "+ serviceType + " for " + actualType);
				return new TrackedListener(serviceType,actualType,l);
			}
		} catch (ClassNotFoundException e) {
			// ok
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Class getServiceType(Class actualType) {
		Aggregate aggr = (Aggregate) actualType.getAnnotation(Aggregate.class);
		if ( aggr != null) {
			return aggr.value();
		}
		
		if ( !Iterable.class.isAssignableFrom(actualType)) {
			return null;			
		}

		for ( Type type : actualType.getGenericInterfaces()) {
			if ( !(type instanceof ParameterizedType)){
				continue;
			}
			ParameterizedType ptype = (ParameterizedType) type;
			Type rawType = ptype.getRawType();
			if ( rawType!= Iterable.class) {
				continue;
			}
			
			return (Class) ptype.getActualTypeArguments()[0];
		}
		return null;
	}

	final Class	serviceType;
	final Class	actualType;
	final ListenerInfo info;

	public TrackedListener(Class serviceType, Class actualType, ListenerInfo info) {
		this.serviceType = serviceType;
		this.actualType = actualType;
		this.info = info;
	}

	@Override
	public String toString() {
		return "TrackedListener [serviceType=" + serviceType + ", actualType=" + actualType + ", info=" + info + "]";
	}

}
