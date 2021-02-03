package biz.aQute.mqtt.paho.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.osgi.dto.DTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import aQute.lib.exceptions.Exceptions;
import biz.aQute.broker.api.Topic;

@Component
public class TopicImpl implements Topic {
	final MqttCentral	central;
	final URI			uri;
	final String		type;
	final String		topic;
	final int			qos;
	final boolean		retain;

	@Activate
	public TopicImpl(@Reference MqttCentral central,
			TopicConfiguration config) throws Exception {
		this.central = central;
		this.uri = new URI(config.broker()).normalize();
		this.type = config.type();
		this.topic = config.topic();
		this.retain = config.retain();
		this.qos = config.qos();
	}

	@Deactivate
	void deactivate() {
		central.bye(this);
	}

	@Override
	public void publish(DTO data) {
		try {

			if (this.type != null && data.getClass().getCanonicalName().equals(this.type))
				throw new IllegalArgumentException(String
						.format("Invalid type, data is of type %s but must be of type %s", data.getClass(), type));

			String payload = toJson(data);

			central.getClient(this, uri).getTopic(topic).publish(payload.getBytes(StandardCharsets.UTF_8), qos, retain);

		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private String toJson(DTO data) {
		try {
			return MqttCentral.codec.enc().put(data).toString();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

}
