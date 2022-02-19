package biz.aQute.mqtt.paho.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import aQute.lib.io.IO;
import biz.aQute.broker.api.Topic;
import biz.aQute.mqtt.paho.client.config.BrokerConfig;
import biz.aQute.osgi.configuration.util.ConfigSetter;
import io.moquette.broker.Server;
import io.moquette.broker.config.FileResourceLoader;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.IResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;

public class ConnectionTest {
	Server		mqttBroker;
	NetProxy	p;

	public ConnectionTest() throws Exception {
	}

	@Before
	public void setup() throws InterruptedException, IOException {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"[%1$tF %1$tT %1$tL] [%4$-7s] %5$s %n");
		setLevel(Level.FINEST);
		IResourceLoader loader = new FileResourceLoader(IO.getFile("resources/config/config.properties"));
		final IConfig classPathConfig = new ResourceLoaderConfig(loader);

		mqttBroker = new Server();
		mqttBroker.startServer(classPathConfig, Collections.emptyList());
		while (mqttBroker.getPort() == 0) {
			System.out.println("still at port 0");
			Thread.sleep(100);
		}
		p = new NetProxy("localhost", mqttBroker.getPort());
	}

	@After
	public void after() {
		mqttBroker.stopServer();
	}

	public static void setLevel(Level targetLevel) {
		Logger root = Logger.getLogger("");
		root.setLevel(targetLevel);
		for (Handler handler : root.getHandlers()) {
			handler.setLevel(targetLevel);
		}
		System.out.println("level set: " + targetLevel.getName());
	}

	@Ignore
	@Test
	public void simpleTest() throws Exception {
		Closeable c = setUpSender();

		ConfigSetter<BrokerConfig> cs = new ConfigSetter<>(BrokerConfig.class);
		BrokerConfig t = cs.delegate();
		cs.set(t.connectionTimeOutInSecs()).to(1);
		cs.set(t.keepAliveInSecs()).to(2);
		cs.set(t.maxReconnectDelaySec()).to(3);
		cs.set(t.name()).to("test");
		cs.set(t.cleanSession()).to(false);
		cs.set(t.uri()).to("tcp://CLIENTID1@localhost:" + mqttBroker.getPort());
		AtomicBoolean	done = new AtomicBoolean(false);
		MqttCentral mqttCentral = new MqttCentral();
		try {

			BrokerImpl facade = new BrokerImpl(mqttCentral, t);
			facade.subscribe(s -> {
				System.out.println("received " + s);
				if ( s.contains("104"))
					done.set(true);
			}, String.class, 1, "foo");
			System.out.println("port : " + mqttBroker.getPort() + " " + p.getPort());
			assertThat(facade.client.getValue().isConnected()).isTrue();

			Thread.sleep(2000);

			facade.client.getValue().setCallback(new MqttCallback() {

				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					System.out.println("message arrived " + message);
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					System.out.println("delivery complete " + token);

				}

				@Override
				public void connectionLost(Throwable cause) {
					System.out.println("connection lost");
				}
			});
			Thread.sleep(2000);
			p.enable(false);
			Thread.sleep(2000);
			p.enable(true);
			Awaitility.await().atMost(30, TimeUnit.SECONDS).until( done::get);

		} finally {
			p.close();
			mqttCentral.deactivate();
			IO.close(c);
		}
	}

	private Closeable setUpSender() throws Exception {
		ConfigSetter<BrokerConfig> cs = new ConfigSetter<>(BrokerConfig.class);
		BrokerConfig t1 = cs.delegate();
		cs.set(t1.connectionTimeOutInSecs()).to(1);
		cs.set(t1.keepAliveInSecs()).to(2);
		cs.set(t1.automaticReconnect()).to(true);
		cs.set(t1.name()).to("test");
		cs.set(t1.uri()).to("tcp://CLIENTID2@localhost:" + p.getPort());
		MqttCentral mqttCentral = new MqttCentral();
		BrokerImpl facade = new BrokerImpl(mqttCentral, t1);

		Topic<String> topic = facade.topic("foo", false, 1, String.class);
		Thread thr = new Thread("pump") {
			int n = 100;

			public void run() {
				try {
					while (!isInterrupted())
						try {
							Thread.sleep(500);
							System.out.println("sending " + n);
							topic.publish("Hello " + n++);
						} catch (Exception e) {
							e.printStackTrace();
							return;
						}
				} finally {
					System.out.println("done publishing");
				}
			}
		};
		thr.start();
		return thr::interrupt;
	}
}
