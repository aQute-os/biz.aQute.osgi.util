package biz.aQute.mqtt.paho.client;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import biz.aQute.broker.api.Subscriber;
import biz.aQute.mqtt.paho.client.config.BrokerConfig;

@Component(service = MqttCentral.class, immediate = true)
public class MqttCentral {
	final static Logger		log		= LoggerFactory.getLogger("biz.aQute.mqtt.paho");
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
	// guard
	final Object					lock				= new Object();
	final Map<URI, Promise<Client>>	clients				= new HashMap<>();
	final PromiseFactory			promiseFactory		= new PromiseFactory(null);
	// all access guarded by MqttCentral.lock
	int								openClients			= 0;
	long							connectionTimeout	= 30000;
	int								retries				= 20;

	// all access guarded by MqttCentral.lock
	class Client {
		final URI			uri;
		final MqttClient	client;
		final Set<Object>	owners	= new HashSet<>();
		final String		uuid	= UUID.randomUUID().toString();

		// all access guarded by MqttCentral.lock
		Client(URI uri, BrokerConfig config) throws InterruptedException {
			int i = 0;
			this.uri = uri;
			String clientId = uri.getUserInfo();
			if (Strings.isEmpty(clientId))
				clientId = uuid;

			try {
				MqttClient client = new MqttClient(uri.toString(), clientId, new MemoryPersistence());
				client.setTimeToWait(config.timeToWait());
				
				while (true) {
					try {

						// TODO persistence
						MqttConnectOptions options = new MqttConnectOptions();
						if (config.username() != null && config.password() != null) {
							options.setUserName(config.username());
							options.setPassword(config.password().toCharArray());
						}

						options.setKeepAliveInterval( config.keepAliveInSecs());
						options.setConnectionTimeout( config.connectionTimeOutInSecs());
						options.setAutomaticReconnect(config.automaticReconnect());
						options.setMaxReconnectDelay(config.maxReconnectDelaySec()*1000);
						options.setCleanSession(false);
						client.connect(options);
						System.out.println("connected " + client.isConnected() + " " + client.getCurrentServerURI());
						break;
					} catch (MqttException e) {
						if (i++ < retries) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e1) {
								log.info("interrupted {}", uri);
								throw e1;
							}
						} else {
							log.error("could not create a connection to {}", uri);
							throw e;
						}

					}
				}
				this.client = client;
				openClients++;
			} catch (MqttException e) {
				e.printStackTrace();
				throw Exceptions.duck(e);
			}
		}

		// all access guarded by MqttCentral.lock
		boolean remove(Object owner) {
			owners.remove(owner);
			if (owners.isEmpty()) {
				System.out.println("clients empty");
				clients.remove(uri);
				promiseFactory.submit(() -> {
					try {
						client.disconnectForcibly();
						client.close(true);
						openClients--;
						lock.notifyAll();
						System.out.println("closed");
					} catch (MqttException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return null;
				});
				return true;
			}
			return false;
		}
	}

	@Deactivate
	void deactivate() {
		clients.values().forEach(p -> p.onSuccess(c -> IO.close(c.client)));
	}

	Promise<MqttClient> getClient(Object owner, URI uri, BrokerConfig config) {

		synchronized (lock) {

			Promise<Client> client = clients.get(uri);
			if (client == null) {
				client = promiseFactory.submit(() -> new Client(uri, config));
				clients.put(uri, client);
			}
			client.onFailure( e -> {
				synchronized (lock) {
					clients.remove(uri);
				}				
				log.warn("failed connection to {}: {}", uri, e, e);
			});

			return client.map(c -> c.client);
		}
	}

	void bye(Object owner) {
		synchronized (lock) {
			Collection<Promise<Client>> values = new HashSet<>(clients.values());
			values.forEach(p -> p.onSuccess(c -> {
				synchronized (lock) {
					c.remove(owner);
				}
			}));
		}
	}

	void sync() throws InterruptedException {
		long deadline = System.currentTimeMillis() + 10000;
		synchronized (lock) {
			while (openClients > 0 && System.currentTimeMillis() < deadline) {
				lock.wait(100);

			}
		}
	}

}
