package biz.aQute.mqtt.paho.client.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface BrokerConfig {

	@AttributeDefinition(description = "The uri of the remote broker in the form of tcp://CLIENTID@localhost:1883")
	String uri();
	
	
	@AttributeDefinition(description = "The name of the connection")
	String name();
	
	@AttributeDefinition(description = "The user name")
	String username();

	/**
	 * This name starts with full stop. Is is available to the component instance but not available as service properties of the registered service. 
	 */
	@AttributeDefinition(name = ".password", type = AttributeType.PASSWORD)
	String password();

	/**
	 * Keep alive. Sends ping at this rate
	 * @return
	 */
	@AttributeDefinition(description = "Number of seconds between keep alive methods")
	int keepAliveInSecs() default 60;
	
	/**
	 * Connection timeout
	 */
	@AttributeDefinition(description = "Number of seconds when a connection is considered broken")
	int connectionTimeOutInSecs() default 120;

	@AttributeDefinition(description = "If this connection should be reconnected if disconnected")
	boolean automaticReconnect() default true;

	@AttributeDefinition(description = "Time to wait in secs before reconnecting")
	int maxReconnectDelaySec() default 120;

	@AttributeDefinition(description = "Starts with a clean session on restart or reconnect. Set to false to support qos 2")
	boolean cleanSession() default true;
}
