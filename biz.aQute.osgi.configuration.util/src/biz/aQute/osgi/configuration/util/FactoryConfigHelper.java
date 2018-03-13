package biz.aQute.osgi.configuration.util;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Extends the {@link ConfigHelper} to support also factories.
 * 
 * @param <T> the type of the configuration (annotation) interface
 */
public class FactoryConfigHelper<T> extends ConfigHelper<T> {

	final String factoryPid;

	/**
	 * Create a configuration helper for a factory PID.
	 * 
	 * @param type the configuration type
	 * @param cm the config admin
	 * @param factoryPid the factory pid
	 */
	public FactoryConfigHelper(Class<T> type, ConfigurationAdmin cm, String factoryPid) {
		super(type, cm);
		this.factoryPid = factoryPid;
	}

	/**
	 * Create a new configuration for the given factory Pid
	 * 
	 * @return this
	 */
	public FactoryConfigHelper<T> create() throws IOException {
		Configuration configuration = cm.createFactoryConfiguration(factoryPid, "?");
		this.pid = configuration.getPid();
		configuration.update(properties);
		return this;
	}

	/**
	 * Answer a list of instances (well the PIDs).
	 * 
	 * @return a list of PIDs that are instances of this factory
	 */
	public Set<String> getInstances() throws IOException, InvalidSyntaxException {
		Configuration[] listConfigurations = cm.listConfigurations("(service.factoryPid="+factoryPid+")");
		if ( listConfigurations == null)
			return Collections.emptySet();
		
		return Stream.of(listConfigurations).map( c -> c.getPid()).collect( Collectors.toSet());
	}
}
