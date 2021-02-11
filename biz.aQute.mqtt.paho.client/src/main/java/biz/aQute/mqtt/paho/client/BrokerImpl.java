package biz.aQute.mqtt.paho.client;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.util.promise.Promise;

import aQute.lib.exceptions.Exceptions;
import biz.aQute.broker.api.Broker;
import biz.aQute.broker.api.Subscriber;
import biz.aQute.broker.api.Topic;
import biz.aQute.mqtt.paho.client.config.BrokerConfig;

@Designate(ocd = BrokerConfig.class, factory = true)
@Component(scope = ServiceScope.PROTOTYPE, name = "biz.aQute.mqtt.paho.client", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class BrokerImpl implements Broker {
	final MqttCentral			central;
	final URI					uri;
	final Promise<MqttClient>	client;
	final BrokerConfig			config;

	@Activate
	public BrokerImpl(@Reference MqttCentral central, BrokerConfig config) throws URISyntaxException {
		this.central = central;
		this.config = config;
		if (config.uri() == null)
			throw new IllegalArgumentException("The uri is a mandatory parameter");

		this.uri = new URI(config.uri()).normalize();
		this.client = this.central.getClient(this, uri, config);
	}

	@Deactivate
	void deactivate() {
		central.bye(this);
	}

	@Override
	public <T> Topic<T> topic(String topic, boolean retain, int qos, Class<T> type) {
		return new TopicImpl<T>(this, topic, retain, qos, type);
	}

	// TODO check for multiple subscriptions to same topic,
	// PAHO can't handle that

	@Override
	public <T> Closeable subscribe(Subscriber<T> subscriber, Class<T> type, int qos, String... topics) {
		if (topics.length == 0)
			return () -> {
			};

		MqttClient client = getClient();

		for (String topic : topics) {
			try {
				client.subscribe(topic, qos, (t, m) -> {
					try {
						T data = MqttCentral.codec.dec().from(m.getPayload()).get(type);
						subscriber.receive(data);
					} catch (Exception e) {
						MqttCentral.log.error("failed to deserialize received object {} for topic {}", m, t, e);
						e.printStackTrace();
					}
				});
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return () -> {
			for (String topic : topics) {
				try {
					client.unsubscribe(topic);
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
	}

	MqttClient getClient() {
		try {
			return central.getClient(this, uri, config).getValue();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw Exceptions.duck(e);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}
}
