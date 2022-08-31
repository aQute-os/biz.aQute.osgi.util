package biz.aQute.aggregate.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.util.tracker.ServiceTracker;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import biz.aQute.aggregate.api.Aggregate;
import biz.aQute.aggregate.api.AggregateConstants;
import biz.aQute.aggregate.provider.TrackedService.ActualTypeRegistration;

@SuppressWarnings("unused")
public class AggregateStateTest {
	static LaunchpadBuilder					builder	= new LaunchpadBuilder().nostart()
		.bndrun("test.bndrun")
		.set(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "2");
	final static AtomicReference<AggReq1>	ref1	= new AtomicReference<>();

	@Before
	public void before() {
		ref1.set(null);
	}

	@Service
	ServiceComponentRuntime scr;

	interface IF {}

	@Component(enabled = false)
	public static class A1 implements IF {

	}

	@Component
	public static class A2 implements IF {

	}

	@Aggregate
	interface AggIF1 extends Iterable<IF> {}

	@Component(immediate = true)
	public static class AggReq1 {
		final List<IF> list;

		@Activate
		public AggReq1(@Reference(policyOption = ReferencePolicyOption.GREEDY)
		List<IF> list) {
			this.list = list;
			ref1.set(this);
		}

		@Reference
		AggIF1 iff;
	}

	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	@Test
	public void straightForwardTest() throws Exception {
		try (Launchpad lp = builder.create()) {
			lp.start();

			System.out.println(
				"Register 2 disabled components to provide the osgi.service Provide-Capability 2x (promised) ");
			Bundle b1 = lp.component(A1.class);
			Bundle b2 = lp.component(A1.class);

			System.out.println("Register an aggregate counter");
			AggregateState aggregateState = new AggregateState(lp.getBundleContext());
			AggregateGogo aggregateGogo = new AggregateGogo();
			aggregateGogo.state = aggregateState;

			ServiceRegistration<AggregateState> register = lp.register(AggregateState.class, aggregateState);
			ServiceRegistration<AggregateGogo> gogo = lp.register(AggregateGogo.class, aggregateGogo,
				"osgi.command.scope", "aQute", "osgi.command.function", new String[] {
					"aggregates"
				});
			await().until(() -> aggregateState.inited);

			System.out.println("Check that we've seen the bundles");
			assertThat(aggregateState.bundles).hasSize(7);

			TrackedService trackedService = aggregateState.trackedServices.get(IF.class);
			assertNotNull(trackedService);

			assertThat(trackedService.promised).isEqualTo(2);
			assertThat(trackedService.discovered).isEqualTo(0);
			assertThat(trackedService.adjust).isEqualTo(0);
			assertThat(trackedService.actualTypes).isEmpty();

			System.out.println("Register our whiteboard server, will add a demand");
			Bundle aggreq = lp.component(AggReq1.class);

			await().until(() -> trackedService.actualTypes.size() == 1);

			ActualTypeRegistration ar = trackedService.actualTypes.get(AggIF1.class);
			assertThat(ar.satisfied).isFalse();
			assertThat(ar.localAdjust).isZero();
			assertThat(trackedService.promised).isEqualTo(2);
			assertThat(trackedService.discovered).isEqualTo(0);

			System.out.println("Register a enabled component. will promise 1 and provide 1");
			Bundle b3 = lp.component(A2.class);

			System.out.println("wait until we see this service");
			await().until(() -> trackedService.discovered == 1);
			assertThat(trackedService.actualTypes.size()).isEqualTo(1);
			assertThat(ar.satisfied).isFalse();
			assertThat(trackedService.promised).isEqualTo(3);
			assertThat(trackedService.discovered).isEqualTo(1);

			System.out.println("Register an IF and check if it is picked up");
			A1 a = new A1();
			ServiceRegistration<IF> ra1 = lp.register(IF.class, a);
			await().until(() -> trackedService.discovered == 2);

			System.out.println("Register another IF and check if it is picked up, which should satisfy");
			ServiceRegistration<IF> ra2 = lp.register(IF.class, a);
			await().until(() -> trackedService.discovered == 3);
			await().until(() -> ar.satisfied);

			System.out.println("Wait for the registration");
			await().until(() -> ar.reg != null);
			await().until(() -> ref1.get() != null);

			System.out.println("Check if registered aggregate is happy");
			AggReq1 aggReq1 = ref1.get();
			assertThat(aggReq1.iff).isNotNull();
			assertThat(aggReq1.list).isNotNull();
			assertThat(aggReq1.list).hasSize(3);

			int n = 0;
			for (IF x : ref1.get().iff) {
				assertThat(x).isNotNull();
				n++;
			}
			assertThat(n).isEqualTo(3);

			System.out.println("Register an extra service, no changes");
			ServiceRegistration<IF> ra_extra = lp.register(IF.class, a);

			await().until(() -> trackedService.discovered == 4);
			assertThat(lp.getBundleContext()
				.getServiceReference(AggIF1.class)).isNotNull();

			System.out.println("Unregister the extra service, should make no difference");
			ra_extra.unregister();
			await().until(() -> trackedService.discovered == 3);
			await().until(() -> lp.getBundleContext()
				.getServiceReference(AggIF1.class) != null);
			assertThat(ar.satisfied).isTrue();
			assertThat(ar.reg).isNotNull();

			n = 0;
			for (IF x : ref1.get().iff) {
				assertThat(x).isNotNull();
				n++;
			}
			assertThat(n).isEqualTo(3);
			ActualTypeFactory remember = ar.reg;
			assertThat(remember.trackers).isNotEmpty();
			ServiceTracker st = ar.reg.trackers.values()
				.iterator()
				.next();

			System.out.println("Remove a service, the actualType will become dissatisfied");
			ra1.unregister();
			await().until(() -> trackedService.discovered == 2);
			await().until(() -> lp.getBundleContext()
				.getServiceReference(AggIF1.class) == null);

			System.out.println("make sure the actual type factory has been cleaned up");
			assertThat(remember.closed).isTrue();
			assertThat(remember.trackers).isEmpty();
			assertThat(st.getTrackingCount()).isEqualTo(-1);

			System.out.println("Remove the A1 bundle that offer 1 instance, so should become happy again");
			assertThat(trackedService.promised).isEqualTo(3);
			b1.uninstall();
			await().until(() -> trackedService.promised == 2);
			await().until(() -> ar.satisfied);

			await().until(() -> lp.getBundleContext()
				.getServiceReference(AggIF1.class) != null);

			n = 0;
			for (IF x : ref1.get().iff) {
				assertThat(x).isNotNull();
				n++;
			}
			assertThat(n).isEqualTo(2);

			System.out.println("Uninstall the white board service & the promised bundles, should clean up");
			aggreq.uninstall();
			await().until(() -> trackedService.actualTypes.isEmpty());
			assertThat(trackedService.promised).isEqualTo(2);
			assertThat(trackedService.discovered).isEqualTo(2);
			assertThat(lp.getBundleContext()
				.getServiceReference(AggIF1.class)).isNull();

			b3.uninstall();
			await().until(() -> trackedService.promised == 1);
			b2.stop();

			System.out.println("Check that no services are tracked & the services is unregistered");
			assertThat(aggregateState.trackedServices.get(IF.class)).isNull();

			register.unregister();
			aggregateState.close();
		}
	}

	final static AtomicReference<AggReq2> ref2 = new AtomicReference<>();

	@Component
	public static class AggReq2 {

		public AggReq2() {
			ref2.set(this);
		}

		@Reference
		AggIF1 iff;
	}

	@Test
	public void test2ReqWithSameAggregateInterface() throws Exception {
		try (Launchpad lp = builder.create()) {
			AggregateState state = new AggregateState(lp.getBundleContext());
			ServiceRegistration<AggregateState> register = lp.register(AggregateState.class, state);
			lp.start();
			System.out.println(
				"Register a disabled component so we can check it picks up offers from prior registered bundles");
			Bundle b1 = lp.component(A1.class);

			System.out.println("Register 2 different components that share the same actual type");
			Bundle a = lp.component(AggReq1.class);
			Bundle b = lp.component(AggReq2.class);

			System.out.println("check if we picked it all up correctly");
			await().until(() -> state.trackedServices.get(IF.class) != null);
			TrackedService ts = state.trackedServices.get(IF.class);
			assertThat(ts).isNotNull();
			assertThat(ts.discovered).isEqualTo(0);
			assertThat(ts.promised).isEqualTo(1);
			assertThat(ts.actualTypes.size()).isEqualTo(1);

			ActualTypeRegistration atr = ts.actualTypes.get(AggIF1.class);
			assertThat(atr).isNotNull();
			assertThat(atr.clients).isEqualTo(2);

			System.out.println("Lets satisfy it");
			ServiceRegistration<IF> a1 = b1.getBundleContext()
				.registerService(IF.class, new A1(), null);

			await().until(() -> atr.satisfied);
			await().until(() -> atr.reg != null);

			assertThat(ref1.get()).isNotNull();
			assertThat(ref2.get()).isNotNull();

			System.out.println("remove one requirerer");
			assertThat(atr.clients).isEqualTo(2);
			b.uninstall();
			assertThat(atr.clients).isEqualTo(1);
			assertThat(atr.satisfied).isTrue();
			assertThat(atr.reg).isNotNull();

			System.out.println("remove the last requirerer");
			a.uninstall();
			assertThat(ts.promised).isEqualTo(1);
			assertThat(atr.clients).isEqualTo(0);
			assertThat(atr.satisfied).isTrue();
			await().until(() -> atr.reg == null);

			b1.uninstall();

			await().until(() -> state.trackedServices.get(IF.class) == null);
			state.close();
		}
	}

	interface AggIF3 extends Iterable<IF> {}

	final static AtomicReference<AggReq3> ref3 = new AtomicReference<>();

	@Component
	public static class AggReq3 {

		{
			ref3.set(this);
		}

		@Reference
		AggIF3 iff;
	}

	@Test
	public void test2ReqWithDifferenceInterface() throws Exception {
		try (Launchpad lp = builder.create()) {
			AggregateState state = new AggregateState(lp.getBundleContext());
			ServiceRegistration<AggregateState> register = lp.register(AggregateState.class, state);
			lp.start();
			System.out.println(
				"Register a disabled component so we can check it picks up offers from prior registered bundles");
			Bundle b1 = lp.component(A1.class);

			System.out.println("Register 2 different components that have different actual types");
			Bundle r1 = lp.component(AggReq1.class);
			Bundle r3 = lp.component(AggReq3.class);

			System.out.println("check if we picked it all up correctly");
			TrackedService ts = state.trackedServices.get(IF.class);
			assertThat(ts).isNotNull();
			assertThat(ts.discovered).isEqualTo(0);
			assertThat(ts.promised).isEqualTo(1);
			assertThat(ts.actualTypes.size()).isEqualTo(2);

			ActualTypeRegistration atr1 = ts.actualTypes.get(AggIF1.class);
			ActualTypeRegistration atr3 = ts.actualTypes.get(AggIF3.class);

			assertThat(atr1).isNotNull();
			assertThat(atr1.clients).isEqualTo(1);
			assertThat(atr3).isNotNull();
			assertThat(atr3.clients).isEqualTo(1);

			System.out.println("Lets satisfy it");
			ServiceRegistration<IF> a1 = b1.getBundleContext()
				.registerService(IF.class, new A1(), null);

			await().until(() -> atr1.satisfied);
			await().until(() -> atr1.reg != null);
			await().until(() -> atr3.satisfied);
			await().until(() -> atr3.reg != null);

			assertThat(ref1.get()).isNotNull();
			assertThat(ref3.get()).isNotNull();

			System.out.println("remove one requirerer");
			assertThat(atr1.clients).isEqualTo(1);
			assertThat(atr3.clients).isEqualTo(1);
			r1.uninstall();
			assertThat(atr1.clients).isEqualTo(0);
			assertThat(atr3.clients).isEqualTo(1);
			assertThat(ts.actualTypes).hasSize(1);

			assertThat(atr3.satisfied).isTrue();
			assertThat(atr3.reg).isNotNull();

			System.out.println("remove the last requirerer");
			r3.uninstall();

			assertThat(ts.promised).isEqualTo(1);
			await().until(() -> atr3.reg == null);

			b1.uninstall();

			await().until(() -> state.trackedServices.get(IF.class) == null);
			state.close();
		}
	}

	@Aggregate(adjust = 1)
	public interface AggIF4 extends Iterable<IF> {}

	@Component
	public static class AggReq4 {

		@Reference
		AggIF4 iff;
	}

	@Test
	public void testAnnotationAdjust() throws Exception {
		try (Launchpad lp = builder.create()) {
			AggregateState state = new AggregateState(lp.getBundleContext());
			ServiceRegistration<AggregateState> register = lp.register(AggregateState.class, state);
			lp.start();
			System.out.println(
				"Register a disabled component so we can check it picks up offers from prior registered bundles");
			Bundle falsePromise = lp.component(A1.class);

			Bundle req = lp.component(AggReq4.class);

			TrackedService ts = state.trackedServices.get(IF.class);
			assertNotNull(ts);
			assertThat(ts.promised).isEqualTo(1);

			ActualTypeRegistration atr4 = ts.actualTypes.get(AggIF4.class);
			assertThat(atr4).isNotNull();

			assertThat(atr4.clients).isEqualTo(1);
			assertThat(atr4.localAdjust).isEqualTo(1);

			falsePromise.getBundleContext()
				.registerService(IF.class, new A1(), null);

			Thread.sleep(100);
			assertThat(atr4.isSatisfied()).isFalse();
			assertThat(ts.discovered).isEqualTo(1);

			ServiceRegistration<IF> last = falsePromise.getBundleContext()
				.registerService(IF.class, new A1(), null);
			assertThat(atr4.isSatisfied()).isTrue();
			assertThat(ts.discovered).isEqualTo(2);

			await().until(() -> atr4.reg != null);

			last.unregister();
			assertThat(atr4.isSatisfied()).isFalse();
			assertThat(ts.discovered).isEqualTo(1);

			state.close();

		}

	}

	@Test
	public void testGlobalAdjust() throws Exception {
		String key = AggregateConstants.PREFIX_TO_ADJUST + IF.class.getName();
		try (Launchpad lp = builder.create()) {
			System.setProperty(key, "2");
			AggregateState state = new AggregateState(lp.getBundleContext());
			ServiceRegistration<AggregateState> register = lp.register(AggregateState.class, state);
			lp.start();
			System.out.println(
				"Register a disabled component so we can check it picks up offers from prior registered bundles");
			Bundle falsePromise = lp.component(A1.class);

			System.out.println("register a requirer");
			Bundle req = lp.component(AggReq1.class);

			TrackedService ts = state.trackedServices.get(IF.class);
			assertThat(ts.promised).isEqualTo(1);

			System.out.println("verify we have picked up the global adjust");

			assertThat(ts.override).isEqualTo(-1);
			assertThat(ts.adjust).isEqualTo(2);
			assertThat(ts.discovered).isEqualTo(0);
			assertThat(ts.actualTypes).hasSize(1);
			ActualTypeRegistration atr = ts.actualTypes.get(AggIF1.class);
			assertThat(atr).isNotNull();
			assertThat(atr.isSatisfied()).isFalse();
			assertThat(atr.localAdjust).isEqualTo(0);
			assertThat(atr.localOverride).isEqualTo(-1);

			System.out.println("register 2 services, should not satisfy");
			ServiceRegistration<IF> s1 = lp.register(IF.class, new A1());
			ServiceRegistration<IF> s2 = lp.register(IF.class, new A1());

			await().until(() -> ts.discovered == 2);
			assertThat(atr.isSatisfied()).isFalse();

			ServiceRegistration<IF> s3 = lp.register(IF.class, new A1());
			await().until(() -> ts.discovered == 3);
			assertThat(atr.isSatisfied()).isTrue();
			await().until(() -> atr.reg != null);

			state.close();

		} finally {
			System.getProperties()
				.remove(key);
		}

	}

	@Test
	public void testGlobalActualTypeOverride() throws Exception {
		String override = AggregateConstants.PREFIX_TO_OVERRIDE + AggIF1.class.getName();
		String adjust = AggregateConstants.PREFIX_TO_ADJUST + AggIF1.class.getName();
		try (Launchpad lp = builder.create()) {
			System.setProperty(override, "2");
			System.setProperty(adjust, "1000");
			AggregateState state = new AggregateState(lp.getBundleContext());
			ServiceRegistration<AggregateState> register = lp.register(AggregateState.class, state);
			lp.start();
			System.out.println(
				"Register a disabled component so we can check it picks up offers from prior registered bundles");
			Bundle falsePromise = lp.component(A1.class);

			System.out.println("register a requirer");
			Bundle req = lp.component(AggReq1.class);

			TrackedService ts = state.trackedServices.get(IF.class);
			assertThat(ts.promised).isEqualTo(1);

			System.out.println("verify we have picked up the global adjust");

			assertThat(ts.promised).isEqualTo(1);
			assertThat(ts.adjust).isEqualTo(0);
			assertThat(ts.override).isEqualTo(-1);
			assertThat(ts.discovered).isEqualTo(0);
			assertThat(ts.actualTypes).hasSize(1);
			ActualTypeRegistration atr = ts.actualTypes.get(AggIF1.class);
			assertThat(atr).isNotNull();
			assertThat(atr.localAdjust).isEqualTo(1000);
			assertThat(atr.localOverride).isEqualTo(2);
			assertThat(atr.isSatisfied()).isFalse();

			System.out.println("register 2 services, should satisfy");
			ServiceRegistration<IF> s1 = lp.register(IF.class, new A1());
			ServiceRegistration<IF> s2 = lp.register(IF.class, new A1());

			await().until(() -> ts.discovered == 2);
			assertThat(atr.isSatisfied()).isTrue();

			ServiceRegistration<IF> s3 = lp.register(IF.class, new A1());
			await().until(() -> ts.discovered == 3);
			assertThat(atr.isSatisfied()).isTrue();
			await().until(() -> atr.reg != null);

			state.close();

		} finally {
			System.getProperties()
				.remove(override);
			System.getProperties()
				.remove(adjust);
		}

	}

	@Test
	public void testAggregateHeader() throws Exception {
		try (Launchpad lp = builder.create()) {
			AggregateState state = new AggregateState(lp.getBundleContext());
			ServiceRegistration<AggregateState> register = lp.register(AggregateState.class, state);
			lp.start();
			System.out.println(
				"Register a disabled component so we can check it picks up offers from prior registered bundles");

			Bundle falsePromise = lp.component(A1.class);
			Bundle aggrPromise3 = lp.bundle()
				.addResource(A1.class)
				.header(AggregateConstants.AGGREGATE_OVERRIDE,
					IF.class.getName() + ";" + AggregateConstants.AGGREGATE_OVERRIDE_PROMISE_ATTR + "=3")
				.start();

			TrackedService ts = state.trackedServices.get(IF.class);
			assertThat(ts.promised).isEqualTo(3 + 1);
			assertThat(ts.actualTypes).isEmpty();

			System.out.println("add an actualType");
			Bundle aggIF1 = lp.bundle()
				.addResource(AggIF1.class)
				.header(AggregateConstants.AGGREGATE_OVERRIDE,
					IF.class.getName() + ";" + AggregateConstants.AGGREGATE_OVERRIDE_ACTUAL_ATTR + "=\""
						+ AggIF1.class.getName() + "," + AggIF1.class.getName() + "\"")
				.start();

			assertThat(ts.promised).isEqualTo(3 + 1);
			assertThat(ts.actualTypes.size()).isEqualTo(1);

			ActualTypeRegistration atr = ts.actualTypes.get(AggIF1.class);
			assertThat(atr.clients).isEqualTo(2);

			state.close();

		}

	}
}
