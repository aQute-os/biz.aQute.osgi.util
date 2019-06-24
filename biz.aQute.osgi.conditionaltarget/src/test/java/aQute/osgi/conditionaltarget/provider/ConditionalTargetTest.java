package aQute.osgi.conditionaltarget.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.runtime.ServiceComponentRuntime;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import aQute.osgi.conditionaltarget.api.ConditionalTarget;

@RunWith(LaunchpadRunner.class)
public class ConditionalTargetTest {

	static LaunchpadBuilder builder = new LaunchpadBuilder().runfw("org.apache.felix.framework;version=6")
			.bundles(
					"org.osgi.util.promise, org.osgi.util.function, org.apache.felix.scr,org.apache.felix.log,org.apache.felix.configadmin, slf4j.api, slf4j.simple, org.assertj.core, org.awaitility, osgi.enroute.hamcrest.wrapper")
			.debug();

	interface Foo {
	}

	Semaphore				s	= new Semaphore(0);

	@Service
	ServiceComponentRuntime	scr;

	@Service
	ConfigurationAdmin		cm;
	@Service
	Launchpad				lp;

	@Component(service = BasicLifeCycle.class, enabled = false)
	public static class BasicLifeCycle {

		@Reference(target = "(#=1)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
		volatile ConditionalTarget<Foo> foos;

		public BasicLifeCycle() {
			System.out.println("X started");
		}
	}

	Foo foo1 = new Foo() {
	};

	@Test
	public void testBasicLifeCycle() throws Exception {
		lp.enable(BasicLifeCycle.class);
		BasicLifeCycle service = lp.waitForService(BasicLifeCycle.class, 1000).get();
		assertThat(service.foos).isNull();

		lp.register(Foo.class, foo1);

		await().atMost(1, TimeUnit.SECONDS).until(() -> service.foos != null);

		List<Foo> services = service.foos.getServices();
		assertThat(services).hasSize(1);
		assertThat(services).containsExactly(foo1);

		ServiceRegistration<Foo> register = lp.register(Foo.class, foo1);
		await().atMost(1, TimeUnit.SECONDS).until(() -> service.foos == null);

		register.unregister();
		await().atMost(1, TimeUnit.SECONDS).until(() -> service.foos != null);
	}

	@Component(service = PropertiesTest.class, enabled = false)
	public static class PropertiesTest {
		@Reference(target = "(&(#>=2)(foo=*)(#foo=*)([unq]foo=*)([avg]foo=*)([sum]foo=*)([min]foo=*)([max]foo=*))", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
		volatile ConditionalTarget<Foo> foos;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testProperties() throws Exception {
		lp.enable(PropertiesTest.class);

		PropertiesTest service = lp.waitForService(PropertiesTest.class, 1000).get();

		lp.register(Foo.class, foo1, "foo", 1);
		lp.register(Foo.class, foo1, "foo", new int[] { 2 });
		lp.register(Foo.class, foo1, "foo", new int[] { 3 });
		lp.register(Foo.class, foo1, "foo", new Integer[] { 2, 3, 1 });

		await().atMost(10, TimeUnit.SECONDS).until(() -> service.foos != null);

		Map<String, Object> props = service.foos.getAggregateProperties();
		System.out.println(props);
		assertThat(props.get("#")).isEqualTo(4);
		assertThat(props.get("T")).isEqualTo(Foo.class.getName());
		assertThat((List<Object>) props.get("foo")).contains(1, 2, 3);
		assertThat(props.get("#foo")).isEqualTo(6);
		assertThat(props.get("[unq]foo")).isEqualTo(3L);
		assertThat(props.get("[avg]foo")).isEqualTo(2.0D);
		assertThat(props.get("[sum]foo")).isEqualTo(12.0D);
		assertThat(props.get("[min]foo")).isEqualTo(1.0D);
		assertThat(props.get("[max]foo")).isEqualTo(3.0D);
	}

	@Component(service = FilterOperatorTest.class, enabled = false)
	public static class FilterOperatorTest {
		@Reference(target = "(&(#>=2)(#<=3)(foo~=A)(#foo=*))", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
		volatile ConditionalTarget<Foo> foos;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPropertiesOperator() throws Exception {
		lp.enable(FilterOperatorTest.class);

		FilterOperatorTest service = lp.waitForService(FilterOperatorTest.class, 1000).get();

		lp.register(Foo.class, foo1, "foo", "a");
		lp.register(Foo.class, foo1, "foo", "b");
		lp.register(Foo.class, foo1, "foo", new String[] { "c", "d" });

		await().atMost(10, TimeUnit.SECONDS).until(() -> service.foos != null);

		Map<String, Object> props = service.foos.getAggregateProperties();
		System.out.println(props);
		assertThat(props.get("#")).isEqualTo(3);
		assertThat(props.get("T")).isEqualTo(Foo.class.getName());
		assertThat((List<Object>) props.get("foo")).contains("a", "b", "c", "d");
		assertThat(props.get("#foo")).isEqualTo(4);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPropertyBooleanTypes() throws Exception {
		lp.enable(PropertiesTest.class);

		PropertiesTest service = lp.waitForService(PropertiesTest.class, 1000).get();

		lp.register(Foo.class, foo1, "foo", true);
		lp.register(Foo.class, foo1, "foo", new boolean[] { true, true });

		await().atMost(10, TimeUnit.SECONDS).until(() -> service.foos != null);

		Map<String, Object> props = service.foos.getAggregateProperties();
		System.out.println(props);
		assertThat(props.get("#")).isEqualTo(2);
		assertThat(props.get("T")).isEqualTo(Foo.class.getName());
		assertThat((List<Object>) props.get("foo")).contains(true, true, true);
		assertThat(props.get("#foo")).isEqualTo(3);
		assertThat(props.get("[max]foo")).isEqualTo(Double.NaN);
		assertThat(props.get("[unq]foo")).isEqualTo(1L);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPropertyBytesArrays() throws Exception {
		lp.enable(PropertiesTest.class);
		
		PropertiesTest service = lp.waitForService(PropertiesTest.class, 1000).get();

		lp.register(Foo.class, foo1, "foo", new byte[] {1,2});
		lp.register(Foo.class, foo1, "foo", new byte[] {3,4});
		
		await().atMost(10, TimeUnit.SECONDS).until(() -> service.foos != null);

		Map<String, Object> props = service.foos.getAggregateProperties();
		System.out.println(props);
		assertThat(props.get("#")).isEqualTo(2);
		assertThat(props.get("T")).isEqualTo(Foo.class.getName());
		assertThat((List<Object>) props.get("foo")).contains(new byte[] {1,2}, new byte []{3,4});
		assertThat(props.get("#foo")).isEqualTo(2);
		assertThat(props.get("[max]foo")).isEqualTo(Double.NaN);
		assertThat(props.get("[unq]foo")).isEqualTo(2L);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPropertyCollections() throws Exception {
		lp.enable(PropertiesTest.class);
		
		PropertiesTest service = lp.waitForService(PropertiesTest.class, 1000).get();

		lp.register(Foo.class, foo1, "foo", Arrays.asList(1,2,3));
		lp.register(Foo.class, foo1, "foo", 4);
		Vector<Integer> v = new Vector<>();
		v.add(5);
		v.add(6);
		lp.register(Foo.class, foo1, "foo", v);
		
		await().atMost(10, TimeUnit.SECONDS).until(() -> service.foos != null);

		Map<String, Object> props = service.foos.getAggregateProperties();
		System.out.println(props);
		assertThat(props.get("#")).isEqualTo(3);
		assertThat(props.get("T")).isEqualTo(Foo.class.getName());
		assertThat((List<Object>) props.get("foo")).contains(1,2,3,4,5,6);
		assertThat(props.get("#foo")).isEqualTo(6);
		assertThat(props.get("[max]foo")).isEqualTo(6D);
		assertThat(props.get("[sum]foo")).isEqualTo(21D);
		assertThat(props.get("[unq]foo")).isEqualTo(6L);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPropertyMixedTypes() throws Exception {
		lp.enable(PropertiesTest.class);

		PropertiesTest service = lp.waitForService(PropertiesTest.class, 1000).get();

		lp.register(Foo.class, foo1, "foo", 1);
		lp.register(Foo.class, foo1, "foo", new boolean[] { true, false });

		await().atMost(10, TimeUnit.SECONDS).until(() -> service.foos != null);

		Map<String, Object> props = service.foos.getAggregateProperties();
		System.out.println(props);
		assertThat(props.get("#")).isEqualTo(2);
		assertThat(props.get("T")).isEqualTo(Foo.class.getName());
		assertThat((List<Object>) props.get("foo")).contains(1, true, false);
		assertThat(props.get("#foo")).isEqualTo(3);
		assertThat(props.get("[unq]foo")).isEqualTo(3L);
	}

	@Component(service = ConfigTest.class, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = "foo", enabled = false)
	public static class ConfigTest {
		@Reference
		ConditionalTarget<Foo> foos;
	}

	@Test
	public void testConfigurationAdmin() throws Exception {
		lp.enable(ConfigTest.class);
		assertThat(lp.waitForService(ConfigTest.class, 100).isPresent()).isFalse();
		Configuration configuration = cm.createFactoryConfiguration("foo", "?");
		Hashtable<String, Object> ht = new Hashtable<>();
		ht.put("foos.target", "(#=1)");
		configuration.update(ht);

		ConditionalTarget<?> fromFramework = lp.waitForService(ConditionalTarget.class, 100000).get();

		Foo foo1 = new Foo() {
		};
		lp.register(Foo.class, foo1, "foo", 1);

		ConfigTest configTest = lp.waitForService(ConfigTest.class, 500).get();
		assertThat(configTest.foos).isNotEqualTo(fromFramework);
		assertThat(configTest.foos.getAggregateProperties()).isEqualTo(fromFramework.getAggregateProperties());

		configuration.delete();
		await().until(() -> lp.waitForService(ConfigTest.class, 100).isPresent() == false);
		await().until(() -> lp.waitForService(ConditionalTarget.class, 100).isPresent() == false);

		assertThat(fromFramework.getAggregateProperties()).isEmpty();
		
		configuration = cm.createFactoryConfiguration("foo", "?");
		ht = new Hashtable<>();
		ht.put("foos.target", "(&(#=2)([avg]foo>=1.5))");
		configuration.update(ht);

		
		Thread.sleep(500);
		
		assertThat(lp.waitForService(ConfigTest.class, 100)).isNotPresent();

		lp.register(Foo.class, foo1, "foo", 3);
		fromFramework = lp.waitForService(ConditionalTarget.class, 1000).get();
		System.out.println(	fromFramework.getAggregateProperties());
		assertThat(lp.waitForService(ConfigTest.class, 2000)).isPresent();
	}

	@Component(service = AggregateStateTest.class, enabled = false)
	public static class AggregateStateTest {
		BundleContext			context;

		@Reference(target = "(&(#sibling>=3)([unq]region>=2)(#=*))")
		ConditionalTarget<Foo>	foos;

		@Activate
		void activate(BundleContext context) {
			this.context = context;
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAggregateStateExample() throws Exception {
		lp.enable(AggregateStateTest.class);
		assertThat(lp.waitForService(AggregateStateTest.class, 100).isPresent()).isFalse();

		Foo foo1 = new Foo() {
		};
		lp.register(Foo.class, foo1, "sibling", "A", "region", "WEST");
		lp.register(Foo.class, foo1, "sibling", "B", "region", "WEST");
		assertThat(lp.waitForService(AggregateStateTest.class, 100).isPresent()).isFalse();
		lp.register(Foo.class, foo1, "sibling", "C", "region", "WEST");
		assertThat(lp.waitForService(AggregateStateTest.class, 100).isPresent()).isFalse();

		lp.register(Foo.class, foo1, "sibling", "D", "region", "EAST");

		assertThat(lp.waitForService(AggregateStateTest.class, 1000).isPresent()).isTrue();

		ConditionalTarget<Foo> service = lp.getService(ConditionalTarget.class).get();

		// Test iterable
		
		int n = 0;
		for ( @SuppressWarnings("unused") Foo sr : service) {
			n++;
		}
		assertThat(n).isEqualTo(4);
		
		Map<ServiceReference<Foo>, Foo> refs = service.getServiceReferences();
		assertThat(refs).hasSize(4);

		Map<String, Object> ap = service.getAggregateProperties();
		assertThat(ap.get("#")).isEqualTo(4);
		assertThat(ap.get("[unq]region")).isEqualTo(2L);
		assertThat(ap.get("#sibling")).isEqualTo(4);
	}

	@Test
	public void testServicesAvailableBeforeComponentDiscovered() throws Exception {
		lp.register(Foo.class, foo1, "sibling", "A", "region", "WEST");
		lp.register(Foo.class, foo1, "sibling", "B", "region", "WEST");
		lp.register(Foo.class, foo1, "sibling", "C", "region", "WEST");
		lp.register(Foo.class, foo1, "sibling", "D", "region", "EAST");
		lp.enable(AggregateStateTest.class);
		assertThat(lp.waitForService(AggregateStateTest.class, 1000).isPresent()).isTrue();
	}

	@Test
	public void testServiceGetCleanup() throws Exception {
		lp.register(Foo.class, foo1, "sibling", "A", "region", "WEST");
		lp.register(Foo.class, foo1, "sibling", "B", "region", "WEST");
		lp.register(Foo.class, foo1, "sibling", "C", "region", "WEST");
		lp.register(Foo.class, foo1, "sibling", "D", "region", "EAST");

		lp.enable(AggregateStateTest.class);
		assertThat(lp.waitForService(AggregateStateTest.class, 1000).isPresent()).isTrue();

		lp.waitForServiceReference(ConditionalTarget.class, 1000).get();
	}
	
	@Component(service=InvalidField.class, enabled=false)
	public static class InvalidField {
		
		@SuppressWarnings("rawtypes")
		@Reference
		private ConditionalTarget foos;
		
	}
	
	@Test
	public void testInvalidField() {
		lp.enable(InvalidField.class);
		
		assertThat( lp.waitForService(InvalidField.class, 1000)).isPresent();
		
	}
}
