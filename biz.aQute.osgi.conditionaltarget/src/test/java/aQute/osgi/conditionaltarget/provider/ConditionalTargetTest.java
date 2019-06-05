package aQute.osgi.conditionaltarget.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.Closeable;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
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
import org.osgi.service.log.LogService;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.osgi.conditionaltarget.api.ConditionalTarget;
import aQute.osgi.conditionaltarget.provider.ConditionalTargetComponent;

public class ConditionalTargetTest {

	final static LaunchpadBuilder builder = new LaunchpadBuilder().runfw("org.apache.felix.framework")
			.bundles("org.apache.felix.scr,org.apache.felix.log,org.apache.felix.configadmin").debug();

	interface Foo {
	}

	static Semaphore s = new Semaphore(0);

	@Component(service = X.class)
	public static class X {

		@Reference(target = "(#=1)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
		volatile ConditionalTarget<Foo> foos;
	}

	@Service
	ServiceComponentRuntime	scr;

	@Service
	LogService				log;

	@Service
	ConfigurationAdmin		cm;

	@Test
	public void testBasicLifeCycle() throws Exception {
		try (Launchpad lp = builder.create().inject(this)) {

			ConditionalTargetComponent p = new ConditionalTargetComponent();
			p.scr = scr;
			p.activate(lp.getBundleContext());

			try (Closeable c = lp.addComponent(X.class)) {
				X service = lp.waitForService(X.class, 1000).get();
				assertThat(service.foos).isNull();

				Foo foo1 = new Foo() {
				};
				lp.register(Foo.class, foo1);
				await().atMost(1, TimeUnit.SECONDS).until(() -> service.foos != null);

				List<Foo> services = service.foos.getServices();
				assertThat(services).hasSize(1);
				assertThat(services).containsExactly(foo1);

				ServiceRegistration<Foo> register = lp.register(Foo.class, foo1);
				await().atMost(1, TimeUnit.SECONDS).until(() -> service.foos == null);

				register.unregister();
				await().atMost(1, TimeUnit.SECONDS).until(() -> service.foos != null);
				p.deactivate();
			}
		}
	}

	@Component(service = PropertiesTest.class)
	public static class PropertiesTest {
		@Reference(target = "(&(#>=2)(foo=*)(#foo=*)([unq]foo=*)([avg]foo=*)([sum]foo=*)([min]foo=*)([max]foo=*))",
				cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
		volatile ConditionalTarget<Foo> foos;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testProperties() throws Exception {
		try (Launchpad lp = builder.create().inject(this)) {

			ConditionalTargetComponent p = new ConditionalTargetComponent();
			p.scr = scr;
			p.activate(lp.getBundleContext());

			try (Closeable c = lp.addComponent(PropertiesTest.class)) {
				PropertiesTest service = lp.waitForService(PropertiesTest.class, 1000).get();
				Foo foo1 = new Foo() {
				};
				lp.register(Foo.class, foo1, "foo", 1);
				lp.register(Foo.class, foo1, "foo", new int[] {2});
				lp.register(Foo.class, foo1, "foo", new int[] {3});
				lp.register(Foo.class, foo1, "foo", new Integer[] {2,3,1});
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

				p.deactivate();
			}
		}
	}

	@Component(service = ConfigTest.class, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = "foo")
	public static class ConfigTest {
		@Reference
		ConditionalTarget<Foo> foos;
	}

	@Test
	public void testConfigurationAdmin() throws Exception {
		try (Launchpad lp = builder.create().inject(this)) {

			ConditionalTargetComponent p = new ConditionalTargetComponent();
			p.scr = scr;
			p.activate(lp.getBundleContext());

			try (Closeable c = lp.addComponent(ConfigTest.class)) {
				assertThat(lp.waitForService(ConfigTest.class, 100).isPresent()).isFalse();
				Configuration cc = cm.createFactoryConfiguration("foo","?");
				Hashtable<String,Object> ht = new Hashtable<>();
				ht.put("foos.target", "(#=1)");
				cc.update(ht);
				
				ConditionalTarget<?> service = lp.waitForService(ConditionalTarget.class, 1000).get();

				Foo foo1 = new Foo() {
				};
				lp.register(Foo.class, foo1, "foo", 1);

				ConfigTest configTest = lp.waitForService(ConfigTest.class, 500).get();
				assertThat(configTest.foos).isNotEqualTo(service);
				assertThat(configTest.foos.getAggregateProperties()).isEqualTo(service.getAggregateProperties());

				cc.delete();
				await().until( () -> lp.waitForService(ConfigTest.class, 100).isPresent()==false);
				
				
				cc = cm.createFactoryConfiguration("foo","?");
				ht = new Hashtable<>();
				ht.put("foos.target", "(&(#=2)([avg]foo>=1.5))");
				cc.update(ht);

				Thread.sleep(500);
				assertThat(lp.waitForService(ConfigTest.class, 100).isPresent()).isFalse();
				
				lp.register(Foo.class, foo1, "foo", 2);
				assertThat(lp.waitForService(ConfigTest.class, 1000).isPresent()).isTrue();
				
				p.deactivate();
			}
		}
	}

	@Component(service = AggregateStateTest.class)
	public static class AggregateStateTest {
		BundleContext context;
		
		@Reference(target="(&(#sibling>=3)([unq]region>=2)(#=*))")
		ConditionalTarget<Foo> foos;
		
		@Activate
		void activate(BundleContext context) {
			this.context=context;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAggregateStateExample() throws Exception {
		try (Launchpad lp = builder.create().inject(this)) {

			ConditionalTargetComponent p = new ConditionalTargetComponent();
			p.scr = scr;
			p.activate(lp.getBundleContext());

			try (Closeable c = lp.addComponent(AggregateStateTest.class)) {
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
				
				Map<ServiceReference<Foo>, Foo> refs = service.getServiceReferences();
				assertThat(refs).hasSize(4);

				Map<String, Object> ap = service.getAggregateProperties();
				assertThat(ap.get("#")).isEqualTo(4);
				assertThat(ap.get("[unq]region")).isEqualTo(2L);
				assertThat(ap.get("#sibling")).isEqualTo(4);
				
				p.deactivate();
			}
		}
	}


	@Test
	public void testServicesAvailableBeforeComponentDiscovered() throws Exception {
		try (Launchpad lp = builder.create().inject(this)) {

			ConditionalTargetComponent p = new ConditionalTargetComponent();
			p.scr = scr;
			p.activate(lp.getBundleContext());

			Foo foo1 = new Foo() {
			};
			lp.register(Foo.class, foo1, "sibling", "A", "region", "WEST");
			lp.register(Foo.class, foo1, "sibling", "B", "region", "WEST");
			lp.register(Foo.class, foo1, "sibling", "C", "region", "WEST");			
			lp.register(Foo.class, foo1, "sibling", "D", "region", "EAST");
			
			try (Closeable c = lp.addComponent(AggregateStateTest.class)) {
				assertThat(lp.waitForService(AggregateStateTest.class, 1000).isPresent()).isTrue();
				p.deactivate();
			}
		}
	}


	@Test
	public void testServiceGetCleanup() throws Exception {
		try (Launchpad lp = builder.create().inject(this)) {

			ConditionalTargetComponent p = new ConditionalTargetComponent();
			p.scr = scr;
			p.activate(lp.getBundleContext());

			Foo foo1 = new Foo() {
			};
			lp.register(Foo.class, foo1, "sibling", "A", "region", "WEST");
			lp.register(Foo.class, foo1, "sibling", "B", "region", "WEST");
			lp.register(Foo.class, foo1, "sibling", "C", "region", "WEST");			
			lp.register(Foo.class, foo1, "sibling", "D", "region", "EAST");
			
			try (Closeable c = lp.addComponent(AggregateStateTest.class)) {
				assertThat(lp.waitForService(AggregateStateTest.class, 1000).isPresent()).isTrue();
				
				lp.waitForServiceReference(ConditionalTarget.class, 1000).get();
				
				p.deactivate();
			}
		}
	}
}
