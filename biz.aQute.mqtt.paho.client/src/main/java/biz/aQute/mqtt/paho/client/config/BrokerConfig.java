package biz.aQute.mqtt.paho.client.config;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface BrokerConfig {

	String uri();
	String name();

}
