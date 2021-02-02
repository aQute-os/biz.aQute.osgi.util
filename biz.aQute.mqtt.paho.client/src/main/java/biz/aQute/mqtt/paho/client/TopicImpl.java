package biz.aQute.mqtt.paho.client;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.osgi.dto.DTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.json.JSONCodec;
import biz.aQute.broker.api.QoS;
import biz.aQute.broker.api.Receipt;
import biz.aQute.broker.api.Topic;

@Component
public class TopicImpl implements Topic {
	Logger						log		= LoggerFactory.getLogger("biz.aQute.mqtt.paho");
	final static JSONCodec		codec	= new JSONCodec();
	final MqttCentral			central;
	final URI					uri;
	final QoS					qos;
	final String				type;
	final String				local;
	final String				remote;
	final boolean				retain;
	final Promise<MqttTopic>	topic;

	@Activate
	public TopicImpl(@Reference MqttCentral central,
			TopicConfiguration config) throws URISyntaxException {
		this.central = central;
		this.uri = new URI(config.url()).normalize();
		this.qos = config.qos();
		this.type = config.type();
		this.local = config.local();
		this.retain = config.retain();
		this.remote = config.remote() == null || config.remote().isEmpty() ? this.local : config.remote();
		this.topic = central.getClient(this, uri).map(client -> {
			return client.getTopic(remote);
		});
	}

	@Deactivate
	void deactivate() {
		central.bye(this);
	}

	@Override
	public Receipt publish(DTO data) {
		try {

			if (this.type != null && data.getClass().getCanonicalName().equals(this.type))
				throw new IllegalArgumentException(String
						.format("Invalid type, data is of type %s but must be of type %s", data.getClass(), type));

			String payload = toJson(data);
			MqttDeliveryToken token = topic.getValue().publish(payload.getBytes(StandardCharsets.UTF_8), qos.ordinal(),
					retain);
			return new Receipt() {
				@Override
				public Optional<String> getMessageId() {
					return Optional.of("" + token);
				}

				@Override
				public QoS qos() {
					return QoS.AT_LEAST_ONCE; // TODO
				}

				@Override
				public Optional<String> sync(long timeout) {
					try {
						token.waitForCompletion(timeout);
						MqttException exception = token.getException();
						if (exception != null)
							throw exception;
						return Optional.empty();
					} catch (MqttException e) {
						return Optional.of(e.getMessage());
					}
				}
			};
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Override
	public <T> Closeable subscribe(Consumer<T> source, Class<T> type) {
		try {
			MqttClient client = central.getClient(this, uri).getValue();
			client.subscribe(local, qos.ordinal(), (topic, msg) -> {
				try {
					T dto = fromJson(type, msg.getPayload());
					source.accept(dto);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});

			return () -> {
				central.getClient(this, uri).onSuccess(m -> m.unsubscribe(remote));
			};
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private String toJson(DTO data) {
		try {
			return codec.enc().put(data).toString();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private <T> T fromJson(Class<T> type, byte[] s) {
		try {
			return codec.dec().from(s).get(type);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

}
