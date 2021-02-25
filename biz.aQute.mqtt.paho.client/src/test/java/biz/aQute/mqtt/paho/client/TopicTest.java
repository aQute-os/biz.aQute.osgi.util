package biz.aQute.mqtt.paho.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.dto.DTO;

import aQute.lib.io.IO;
import biz.aQute.broker.api.Subscriber;
import biz.aQute.broker.api.Topic;
import biz.aQute.mqtt.paho.client.config.BrokerConfig;
import biz.aQute.osgi.configuration.util.ConfigSetter;
import io.moquette.broker.Server;
import io.moquette.broker.config.FileResourceLoader;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.IResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;

public class TopicTest {
	Server					mqttBroker;

	@Before
	public void setup() throws InterruptedException, IOException {
		IResourceLoader loader = new FileResourceLoader(IO.getFile("resources/config/config.properties"));
		final IConfig classPathConfig = new ResourceLoaderConfig(loader);

		mqttBroker = new Server();
		mqttBroker.startServer(classPathConfig, Collections.emptyList());
	}

	@After
	public void after() {
		mqttBroker.stopServer();
	}

	public static class TestDTO extends DTO {
		public String	foo;
		public boolean	bar;
	}

	@Test
	public void testSimple() throws Exception {
		ConfigSetter<BrokerConfig> cs = new ConfigSetter<>(BrokerConfig.class);
		BrokerConfig t = cs.delegate();
		cs.set(t.name()).to("test");
		cs.set(t.uri()).to("tcp://CLIENTID@localhost:" + mqttBroker.getPort());


		MqttCentral mqttCentral = new MqttCentral();
		try {
			List<TestDTO> dtos = new CopyOnWriteArrayList<>();

			BrokerImpl facade = new BrokerImpl(mqttCentral, t);
			System.out.println("port : " + mqttBroker.getPort());
			Subscriber<TestDTO> subscriber = new Subscriber<TestDTO>() {
				@Override
				public void receive(TestDTO data) {
					System.out.println(data);
					dtos.add(data);
				}
			};

			facade.subscribe(subscriber, TestDTO.class, 0, "foo");

			Topic<TestDTO> topicImpl = facade.topic("foo", false, 0, TestDTO.class);
			TestDTO test = new TestDTO();
			test.bar = true;
			test.foo = "Foo";
			topicImpl.publish(test);

			Awaitility.await().until(() -> !dtos.isEmpty());
			
			assertThat(mqttCentral.clients).hasSize(1);
			
			facade.deactivate();

			Awaitility.await().until(() ->  mqttCentral.clients.isEmpty());
			
		} finally {
			mqttCentral.deactivate();
		}
	}

	@Test(expected = InvocationTargetException.class)
	public void testServerDelay() throws Exception {
		ConfigSetter<BrokerConfig> cs = new ConfigSetter<>(BrokerConfig.class);
		BrokerConfig t = cs.delegate();
		cs.set(t.name()).to("test");
		int nonExistentServerPort = 100;
		cs.set(t.uri()).to("tcp://CLIENTID@localhost:" + nonExistentServerPort);


		MqttCentral mqttCentral = new MqttCentral();
		mqttCentral.retries = 2;
		try {
			BrokerImpl facade = new BrokerImpl(mqttCentral, t);
			facade.getClient();
		} finally {
			mqttCentral.deactivate();
		}
	}
}
