package biz.aQute.mqtt.paho.client;

public @interface TopicConfiguration {
	String topic();

	String broker();

	String type();

	boolean retain() default false;

	int qos() default 0;
}
