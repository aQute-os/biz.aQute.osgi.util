package biz.aQute.mqtt.paho.client;

import biz.aQute.broker.api.QoS;

public @interface TopicConfiguration {
	String local();
	String url();
	String remote() default "";
	QoS qos() default QoS.AT_LEAST_ONCE;
	String type();
	boolean retain();
}
