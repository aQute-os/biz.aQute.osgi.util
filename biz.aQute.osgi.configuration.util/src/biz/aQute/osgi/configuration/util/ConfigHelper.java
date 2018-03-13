package biz.aQute.osgi.configuration.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.annotations.AttributeDefinition;

import aQute.lib.converter.Converter;

/**
 * Provides a helper class to read and update simple configurations in a type
 * safe way. It is based on the idea of Mockito et. al. By calling a method on
 * the configuration proxy interface we keep the method until the next
 * invocation of the {@link #set(Object, Object)} method. In the
 * {@link #set(Object, Object)} method we can then assign the value to
 * properties after converting it to the return type of the method.
 * <p>
 * Method names are mangled according to the OSGi spec.
 */

public class ConfigHelper<T> {

	final T							delegate;
	final Class<T>					type;
	final Hashtable<String, Object>	properties	= new Hashtable<>();
	final ConfigurationAdmin		cm;

	Method							lastInvocation;
	String							pid;

	/**
	 * Create a Config Helper for simple configurations.
	 * 
	 * @param type
	 *            the type of the configuration interface
	 * @param cm
	 *            the Configuration Admin service
	 */
	@SuppressWarnings("unchecked")
	public ConfigHelper(Class<T> type, ConfigurationAdmin cm) {
		this.type = type;
		this.cm = cm;
		this.delegate = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, this::invoke);
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

	/**
	 * Return the proxy. Very short name so we it is not in the way in the set
	 * method.
	 * <p>
	 * The proxy methods will return the live value of the corresponding
	 * property.
	 * 
	 * @return the proxy
	 */
	public T d() {
		return delegate;
	}

	/**
	 * Set the value based on the last invocation. This method is supposed to be
	 * used like:
	 * 
	 * <pre>
	 * ch.set(ch.d().port(), 10000);
	 * </pre>
	 * 
	 * The last invocation on the proxy is remembered and used in the set method
	 * to get the name and return type of the property.
	 * 
	 * @param older
	 *            ignored
	 * @param newer
	 *            the value to set
	 * @return this
	 */
	public <X> ConfigHelper<T> set(X older, X newer) throws Exception {
		assert lastInvocation != null : "Missing invocation of target interface";

		String key = Converter.mangleMethodName(lastInvocation.getName());
		Object value = Converter.cnv(lastInvocation.getGenericReturnType(), newer);
		properties.put(key, value);
		lastInvocation = null;
		return this;
	}

	/**
	 * Get the properties
	 * 
	 * @return the properties
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}

	/**
	 * Read a configuration
	 * 
	 * @param pid
	 *            a non-null pid
	 * @return the properties read or empty if did not exist
	 */
	public Map<String, Object> read(String pid) throws IOException {

		assert pid != null : "Must specify a PID";

		this.pid = pid;
		properties.clear();
		Configuration configuration = cm.getConfiguration(pid, "?");
		Dictionary<String, Object> dict = configuration.getProperties();
		if (dict != null) {
			for (String key : Collections.list(dict.keys())) {
				this.properties.put(key, dict.get(key));
			}
		}
		return properties;
	}

	/**
	 * Update the current configuration. This requires a {@link #read(String)}
	 * to set the proper PID.
	 * 
	 * @throws IOException
	 */
	public void update() throws IOException {
		assert pid != null : "First read the pid before you update";
		Configuration configuration = cm.getConfiguration(pid, "?");
		configuration.update(properties);
	}
	
	/**
	 * Clear the properties
	 */

	public void clear() {
		this.properties.clear();
	}
}
