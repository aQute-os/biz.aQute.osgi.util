package biz.aQute.mqtt.paho.client;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.util.Strings;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;

@Component(service = MqttCentral.class)
class MqttCentral {

	// all access guarded by MqttCentral.clients
	class Client {
		final URI				uri;
		final MqttClient		client;
		final Set<TopicImpl>	clients	= new HashSet<>();
		final String			uuid	= UUID.randomUUID().toString();

		Client(URI uri) {
			try {
				String clientId = uri.getUserInfo();
				if (Strings.isEmpty(clientId))
					clientId = uuid;
				this.uri = uri;
				// TODO persistence
				this.client = new MqttClient(uri.toString(), clientId, new MemoryPersistence());
				MqttConnectOptions options = new MqttConnectOptions();
				options.setAutomaticReconnect(true);
				options.setCleanSession(false);
				client.connect(options);
			} catch (MqttException e) {
				throw Exceptions.duck(e);
			}
		}

		boolean remove(TopicImpl topicImpl) {
			clients.remove(topicImpl);
			if (clients.isEmpty()) {
				IO.close(client);
				return true;
			}
			return false;
		}

	}

	// guard
	final Map<URI, Client>	clients				= new HashMap<>();

	long					connectionTimeout	= 30000;
	final PromiseFactory	promiseFactory;

	@Activate
	public MqttCentral(@Reference PromiseFactory promiseFactory) {
		this.promiseFactory = promiseFactory;
	}

	Promise<MqttClient> getClient(TopicImpl cl, URI uri) {
		return promiseFactory.submit(() -> {
			synchronized (clients) {
				Client client = clients.computeIfAbsent(uri.normalize(), Client::new);
				client.clients.add(cl);
				return client.client;
			}
		});
	}

	void bye(TopicImpl topicImpl) {
		synchronized (clients) {
			clients.values().removeIf(c -> {
				return c.remove(topicImpl);
			});
		}
	}

}
