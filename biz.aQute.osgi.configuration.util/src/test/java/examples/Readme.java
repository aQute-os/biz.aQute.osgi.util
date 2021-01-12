package examples;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import biz.aQute.osgi.configuration.util.ConfigHelper;

/**
 * ${snippet includeImports=false, id=myFullOnType, title=ConfigHelper,
 * description="This shows how the ConfigHelper could be used in a Unit-Test."}
 */
public class Readme {

	ConfigurationAdmin cm;

	@interface FooConfig {
		int port() default 10;

		String host() default "localhost";
	}

	public void testSimple() throws Exception {
		ConfigHelper<FooConfig> ch = new ConfigHelper<>(FooConfig.class, cm);
		Map<String, Object> read = ch.read("foo.bar");
		assertEquals(0, read.size());

		assertEquals(10, ch.d()
			.port());
		assertEquals("localhost", ch.d()
			.host());

		ch.set(ch.d()
			.port(), 3400);
		ch.set(ch.d()
			.host(), "example.com");
		ch.update();

		Configuration c = cm.getConfiguration("foo.bar");
		Dictionary<String, Object> properties = c.getProperties();
		assertEquals(3400, properties.get("port"));
		assertEquals("example.com", properties.get("host"));

	}
}
