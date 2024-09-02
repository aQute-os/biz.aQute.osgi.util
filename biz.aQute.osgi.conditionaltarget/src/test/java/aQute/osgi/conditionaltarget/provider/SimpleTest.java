package aQute.osgi.conditionaltarget.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.launchpad.junit.LaunchpadRunner;
import aQute.osgi.conditionaltarget.api.ConditionalTarget;

@RunWith(LaunchpadRunner.class)
@SuppressWarnings("rawtypes")
public class SimpleTest {

	static LaunchpadBuilder	builder	= new LaunchpadBuilder().snapshot()
		.runfw("org.apache.felix.framework;version=")
			.bundles(
					"org.osgi.util.promise, org.osgi.util.function, org.apache.felix.scr,org.apache.felix.log,org.apache.felix.configadmin, slf4j.api, slf4j.simple, org.assertj.core, org.awaitility, osgi.enroute.hamcrest.wrapper")
			.debug();

	@Service
	Launchpad				lp;

	@Component(enabled=false)
	public static class A {

		@Reference
		ConditionalTarget<String> a;
	}

	@Test
	public void testBasicRegistrationOfCTWhenThereIsACTReference() throws InterruptedException {
		lp.enable(A.class);
		Bundle bundle = FrameworkUtil.getBundle(SimpleTest.class);
		assertThat(bundle.getBundleId()).isNotEqualTo(0);

		Optional<ConditionalTarget> s = lp.waitForService(ConditionalTarget.class, 1000);
		assertThat(s.isPresent()).isTrue();

	}

	@Component(enabled=false)
	public static class B {

		@Reference(target="(#=1)")
		ConditionalTarget<String> a;
	}

	@Test
	public void withFilter() throws InterruptedException {
		lp.enable(B.class);

		Optional<ConditionalTarget> s = lp.waitForService(ConditionalTarget.class, 100);
		assertThat(s.isPresent()).isFalse();

		ServiceRegistration<String> register = lp.register(String.class, "B'");

		s = lp.waitForService(ConditionalTarget.class, 1000);
		assertThat(s.isPresent()).isTrue();

		register.unregister();

	}
}
