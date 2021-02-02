package biz.aQute.osgi.configuration.util;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Hashtable;

import org.osgi.service.metatype.annotations.AttributeDefinition;

import aQute.lib.converter.Converter;

public class ConfigSetter<T> {
	final T							delegate;
	final Class<T>					type;
	final Hashtable<String, Object>	properties	= new Hashtable<>();

	Method							lastInvocation;

	@SuppressWarnings("unchecked")
	public ConfigSetter(Class<T> type) {
		this.delegate = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, this::invoke);
		this.type = type;
	}


	public class Setter<X> {

		public ConfigSetter<T> to(X newer) throws Exception {
			assert lastInvocation != null : "Missing invocation of target interface";

			String key = Converter.mangleMethodName(lastInvocation.getName());
			Object value = Converter.cnv(lastInvocation.getGenericReturnType(), newer);
			properties.put(key, value);
			lastInvocation = null;
			return ConfigSetter.this;
		}
	}

	public <X> Setter<X> set(X result) {
		return new Setter<>();
	}

	/*
	 * Invocation for a method on the configuration's interface proxy
	 */
	private Object invoke(Object proxt, Method method, Object... args) throws Exception {
		this.lastInvocation = method;

		String name = Converter.mangleMethodName(method.getName());
		Object value = properties.get(name);
		if (value == null)
			value = method.getDefaultValue();

		if (value == null) {
			AttributeDefinition def = method.getAnnotation(AttributeDefinition.class);
			if (def != null) {
				value = def.defaultValue();
			}
		}

		return Converter.cnv(method.getGenericReturnType(), value);
	}

	public T delegate() {
		return delegate;
	}

}
