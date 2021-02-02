package biz.aQute.mqtt.paho.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.dto.DTO;
import org.osgi.util.promise.PromiseFactory;

import aQute.lib.io.IO;
import biz.aQute.osgi.configuration.util.ConfigSetter;
import io.moquette.broker.Server;
import io.moquette.broker.config.FileResourceLoader;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.IResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;

public class TopicTest {
	final PromiseFactory pf = new PromiseFactory(null);
	Server mqttBroker;

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

	@Ignore
	@Test
	public void testSimple() throws Exception {
		ConfigSetter<TopicConfiguration> cs = new ConfigSetter<>(TopicConfiguration.class);
		TopicConfiguration t = cs.delegate();
		cs.set(t.local()).to("local");
		cs.set(t.remote()).to("remote");
		cs.set(t.retain()).to(true);
		cs.set(t.url()).to("tcp://CLIENTID@localhost:" + mqttBroker.getPort());

		MqttCentral mqttCentral = new MqttCentral(pf);

		TopicImpl ti = new TopicImpl(mqttCentral, t);
		List<TestDTO> dtos = new CopyOnWriteArrayList<>();

		try (Closeable subscribe = ti.subscribe(d -> {
			System.out.println(d);
			dtos.add(d);
		}, TestDTO.class)) {

			Thread.sleep(1000);
			TestDTO test = new TestDTO();
			test.bar = true;
			test.foo = "Foo";
			ti.publish(test);

			Awaitility.await().until(() -> !dtos.isEmpty());
		}
	}
}
