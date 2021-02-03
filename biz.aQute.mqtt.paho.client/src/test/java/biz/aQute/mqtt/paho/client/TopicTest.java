package biz.aQute.mqtt.paho.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.dto.DTO;

import aQute.lib.io.IO;
import biz.aQute.broker.api.Subscriber;
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
		ConfigSetter<TopicConfiguration> cs = new ConfigSetter<>(TopicConfiguration.class);
		TopicConfiguration t = cs.delegate();
		cs.set(t.topic()).to("remote");
		cs.set(t.retain()).to(true);
		String broker = "tcp://CLIENTID@localhost:" + mqttBroker.getPort();
		cs.set(t.broker()).to(broker);

		MqttCentral mqttCentral = new MqttCentral();
		try {
			List<TestDTO> dtos = new CopyOnWriteArrayList<>();

			Subscriber<TestDTO> subscriber = new Subscriber<TestDTO>() {
				@Override
				public void receive(TestDTO data) {
					System.out.println(data);
					dtos.add(data);
				}
			};
			
			Map<String, Object> sp = new HashMap<>();
			sp.put("broker", broker);
			sp.put("topics", "remote");
			mqttCentral.addSubscriber(subscriber, sp);

			TopicImpl topicImpl = new TopicImpl(mqttCentral, t);
			TestDTO test = new TestDTO();
			test.bar = true;
			test.foo = "Foo";
			topicImpl.publish(test);

			Awaitility.await().until(() -> !dtos.isEmpty());
			
			assertThat(mqttCentral.clients).hasSize(1);
			
			mqttCentral.removeSubscriber(subscriber);
			
			assertThat(mqttCentral.clients).hasSize(1);
			topicImpl.deactivate();
			assertThat(mqttCentral.clients).hasSize(0);

			
		} finally {
			mqttCentral.deactivate();
		}
	}
}
