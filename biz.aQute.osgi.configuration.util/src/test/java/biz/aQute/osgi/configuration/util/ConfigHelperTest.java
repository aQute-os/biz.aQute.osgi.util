package biz.aQute.osgi.configuration.util;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;

@RunWith(LaunchpadRunner.class)
public class ConfigHelperTest {

	static LaunchpadBuilder			builder	= new LaunchpadBuilder().runfw("org.apache.felix.framework")
			.bundles("org.apache.felix.configadmin").debug();

	@Service
	ConfigurationAdmin	cm;

	@interface FooConfig {
		int port() default 10;

		String host() default "localhost";
	}

	@Test
	public void testSimple() throws Exception {
		ConfigHelper<FooConfig> ch = new ConfigHelper<>(FooConfig.class, cm);
		Map<String, Object> read = ch.read("foo.bar");
		assertEquals(0, read.size());

		assertEquals(10, ch.d().port());
		assertEquals("localhost", ch.d().host());

		ch.set(ch.d().port(), 3400);
		ch.set(ch.d().host(), "example.com");
		ch.update();

		Configuration c = cm.getConfiguration("foo.bar");
		Dictionary<String, Object> properties = c.getProperties();
		assertEquals(3400, properties.get("port"));
		assertEquals("example.com", properties.get("host"));

	}

	@Test
	public void testFactory() throws Exception {
		FactoryConfigHelper<FooConfig> ch = new FactoryConfigHelper<>(FooConfig.class, cm, "foo.bars");

		assertEquals(0, ch.getInstances().size());

		ch.set(ch.d().port(), 3400);
		ch.set(ch.d().host(), "example.com");
		ch.create();
		assertEquals(1, ch.getInstances().size());

		Map<String, Object> read = ch.read(ch.getInstances().iterator().next());
		assertEquals(4, read.size());

		assertEquals(3400, ch.d().port());
		assertEquals("example.com", ch.d().host());
	}
}
