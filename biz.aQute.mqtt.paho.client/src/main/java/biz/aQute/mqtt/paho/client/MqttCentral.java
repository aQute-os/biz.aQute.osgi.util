package biz.aQute.mqtt.paho.client;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.util.Strings;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.collections.MultiMap;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import biz.aQute.broker.api.Subscriber;

@Component(service = MqttCentral.class, immediate = true)
class MqttCentral {
	Logger					log		= LoggerFactory.getLogger("biz.aQute.mqtt.paho");
	final static JSONCodec	codec	= new JSONCodec();

	final static Method		receive;
	static {
		try {
			receive = Subscriber.class.getMethod("receive", Object.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			throw Exceptions.duck(e);
		}

	}

	// all access guarded by MqttCentral.lock
	class Client {
		final URI			uri;
		final MqttClient	client;
		final Set<Object>	clients	= new HashSet<>();
		final String		uuid	= UUID.randomUUID().toString();

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

		boolean remove(Object owner) {
			clients.remove(owner);
			if (clients.isEmpty()) {
				IO.close(client);
				return true;
			}
			return false;
		}

	}

	// guard
	final Object						lock				= new Object();
	final Map<URI, Client>				clients				= new HashMap<>();

	long								connectionTimeout	= 30000;
	final Map<Subscriber<?>, Assistant>	subscribers			= new HashMap<>();
	final MultiMap<String, Assistant>	callbacks			= new MultiMap<>();

	
	
	
	@Deactivate
	void deactivate() {
		clients.values().removeIf(c -> {
			IO.close(c.client);
			return true;
		});
	}

	MqttClient getClient(Object owner, URI uri) {
		synchronized (lock) {
			Client client = clients.computeIfAbsent(uri, Client::new);
			client.clients.add(owner);
			return client.client;
		}
	}

	void bye(Object topicImpl) {
		synchronized (clients) {
			clients.values().removeIf(c -> {
				return c.remove(topicImpl);
			});
		}
	}

	private Closeable subscribe(URI broker, String topic, Assistant assistant) throws Exception {

		synchronized (lock) {
			if (!callbacks.containsKey(topic)) {
				getClient(assistant, broker).subscribe(topic, 1, (tpc, message) -> {
					synchronized (lock) {
						receive(tpc, message.getPayload());
					}
				});
			}
			callbacks.add(topic, assistant);
		}

		return () -> {
			synchronized (lock) {
				if (callbacks.remove(topic, assistant) && callbacks.isEmpty()) {
					try {
						getClient(assistant, assistant.broker).unsubscribe(topic);
					} catch (MqttException e) {
						// ignore
					}
				}
			}

		};
	}

	public void receive(String topic, byte[] payload) {
		synchronized (lock) {
			callbacks.getOrDefault(topic, Collections.emptyList()).forEach(assistant -> assistant.receive(payload));
		}
	}

	class Assistant {

		final Subscriber<?>		subscriber;
		final URI				broker;
		final List<Closeable>	subscriptions	= new ArrayList<>();
		final Type				type;

		Assistant(Subscriber<?> subscriber, String broker, String[] topics) throws Exception {
			this.subscriber = subscriber;
			this.broker = new URI(broker).normalize();
			this.type = getType(subscriber.getClass());
			for (String topic : topics) {
				Closeable subscription = subscribe(this.broker, topic, this);
				subscriptions.add(subscription);
			}
		}

		private Type getType(Class<?> clazz) {
			if (clazz == null)
				throw new IllegalArgumentException(
						"The subscriber must implement the Subscriber<T> type to participate");

			for (Type t : clazz.getGenericInterfaces()) {
				if (t instanceof ParameterizedType) {
					if (((ParameterizedType) t).getRawType() == Subscriber.class)
						return ((ParameterizedType) t).getActualTypeArguments()[0];
				}
			}
			return getType(clazz.getSuperclass());
		}

		public void receive(byte[] payload) {
			try {
				Object object = codec.dec().from(payload).get(type);
				receive.invoke(subscriber, object);
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		void close() {
			subscriptions.forEach(IO::close);
			bye(this);
		}

	}

	@Reference(target = "(&(broker=*)(topics=*))", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addSubscriber(Subscriber<?> subscriber, Map<String, Object> serviceProperties) throws Exception {
		String broker = Converter.cnv(String.class, serviceProperties.get(Subscriber.broker));
		String[] topics = Converter.cnv(String[].class, serviceProperties.get(Subscriber.topics));
		Assistant assistant = new Assistant(subscriber, broker, topics);
		synchronized (lock) {
			subscribers.put(subscriber, assistant);
		}
	}

	void removeSubscriber(Subscriber<?> subscriber) {
		synchronized (lock) {
			subscribers.remove(subscriber).close();
		}
	}

}
