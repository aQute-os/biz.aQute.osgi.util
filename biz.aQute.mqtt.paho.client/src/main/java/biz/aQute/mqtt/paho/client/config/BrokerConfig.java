package biz.aQute.mqtt.paho.client.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface BrokerConfig {

	String uri();
	String name();
	
	String username();

	/**
	 * This name starts with full stop. Is is available to the component instance but not available as service properties of the registered service. 
	 */
	@AttributeDefinition(name = ".password", type = AttributeType.PASSWORD)
	String password();

}
