package biz.aQute.mqtt.paho.client;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.ByteBufferOutputStream;
import biz.aQute.broker.api.Topic;

public class TopicImpl<T> implements Topic<T> {
	final String		topic;
	final int			qos;
	final boolean		retain;
	private BrokerImpl	broker;
	@SuppressWarnings("unused")
	private Class<T>	type;

	public TopicImpl(BrokerImpl brokerImpl, String topic, boolean retain, int qos, Class<T> type) {
		this.broker = brokerImpl;
		this.topic = topic;
		this.retain = retain;
		this.qos = qos;
		this.type = type;
	}

	private byte[] toJson(T data) {
		try {
			ByteBufferOutputStream bbos = new ByteBufferOutputStream();
			MqttCentral.codec.enc().writeDefaults().to(bbos).put(data).flush();
			return bbos.toByteArray();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Override
	public void publish(T data) {
		try {
			byte[] json = toJson(data);
			MqttClient client = broker.getClient();
			client.publish(topic, json, qos, retain);
		} catch (MqttException e) {
			throw Exceptions.duck(e);
		}
	}

}
