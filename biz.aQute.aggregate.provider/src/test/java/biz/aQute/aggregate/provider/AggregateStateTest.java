package biz.aQute.aggregate.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
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
import biz.aQute.aggregate.api.AggregateSettings;
import biz.aQute.aggregate.provider.TrackedService.ActualType;
import biz.aQute.aggregate.provider.TrackedService.BundleInfo;

@SuppressWarnings("unused")
public class AggregateStateTest {

	static {
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "debug");
	}

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

	@Component(enabled = false)
	public static class A1 implements IF {

	}

	@Component
	public static class A2 implements IF {

	}

	interface AggIF1 extends Aggregate<IF> {}

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
			assertThat(aggregateState.bundles).isNotEmpty();

			TrackedService trackedService = aggregateState.trackedServices.get(IF.class);
			assertNotNull(trackedService);

			assertThat(trackedService.bundleInfos.size()).isEqualTo(2);
			assertThat(trackedService.satisfied()
				.size()).isEqualTo(0);
			assertThat(trackedService.actualTypes).isEmpty();

			System.out.println("Register our whiteboard server, will add a demand");
			Bundle aggreq = lp.component(AggReq1.class);

			await().until(() -> trackedService.actualTypes.size() == 1);

			ActualType ar = trackedService.actualTypes.get(AggIF1.class);
			assertThat(ar.satisfied).isFalse();
			assertThat(trackedService.bundleInfos.size()).isEqualTo(2);
			assertThat(trackedService.satisfied()
				.size()).isEqualTo(0);

			System.out.println("Register a enabled component. will promise 1 and provide 1");
			Bundle b3 = lp.component(A2.class);

			System.out.println("wait until we see this service");
			await().until(() -> trackedService.satisfied()
				.size() == 1);
			assertThat(trackedService.actualTypes.size()).isEqualTo(1);
			assertThat(ar.isSatisfied()).isFalse();
			assertThat(trackedService.bundleInfos.size()).isEqualTo(3);
			assertThat(trackedService.satisfied()
				.size()).isEqualTo(1);

			System.out.println("Register the IF promised by b1 and check if it is picked up");
			A1 a = new A1();
			ServiceRegistration<IF> ra1 = b1.getBundleContext()
				.registerService(IF.class, a, null);
			await().until(() -> trackedService.satisfied()
				.size() == 2);

			System.out.println("Register the IF for b2 and check if it is picked up, which should satisfy");
			ServiceRegistration<IF> ra2 = b2.getBundleContext()
				.registerService(IF.class, new A1(), null);
			await().until(() -> trackedService.satisfied()
				.size() == 3);
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
			for (IF x : ref1.get().iff.getServices()) {
				assertThat(x).isNotNull();
				n++;
			}
			assertThat(n).isEqualTo(3);

			System.out.println(
				"Register an extra rogue service. No changes but we now have a dynamically discovered bundleinfo");
			ServiceRegistration<IF> rogue = lp.register(IF.class, a);

			await().until(() -> trackedService.bundleInfos.size() == 4);

			assertThat(lp.getBundleContext()
				.getServiceReference(AggIF1.class)).isNotNull();

			System.out
				.println("Unregister the extra service, should purge the BI but make no difference for our requirer");
			rogue.unregister();
			await().until(() -> trackedService.bundleInfos.size() == 3);
			assertThat(ar.isSatisfied()).isTrue();

			n = 0;
			for (IF x : ref1.get().iff.getServices()) {
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
			await().until(() -> !ar.isSatisfied());
			await().until(() -> lp.getBundleContext()
				.getServiceReference(AggIF1.class) == null);

			System.out.println("make sure the actual type factory has been cleaned up");
			assertThat(remember.closed).isTrue();
			assertThat(remember.trackers).isEmpty();
			assertThat(st.getTrackingCount()).isEqualTo(-1);

			System.out.println("Remove the A1 bundle that offer 1 instance, so should become happy again");
			assertThat(trackedService.bundleInfos.size()).isEqualTo(3);
			b1.uninstall();
			await().until(() -> trackedService.bundleInfos.size() == 2);
			await().until(() -> ar.satisfied);

			await().until(() -> lp.getBundleContext()
				.getServiceReference(AggIF1.class) != null);

			n = 0;
			for (IF x : ref1.get().iff.getServices()) {
				assertThat(x).isNotNull();
				n++;
			}
			assertThat(n).isEqualTo(2);

			System.out.println("Uninstall the white board service & the promised bundles, should clean up");
			aggreq.uninstall();
			await().until(() -> trackedService.actualTypes.isEmpty());
			assertThat(trackedService.bundleInfos.size()).isEqualTo(2);
			assertThat(trackedService.satisfied()
				.size()).isEqualTo(2);
			assertThat(lp.getBundleContext()
				.getServiceReference(AggIF1.class)).isNull();

			b3.uninstall();
			await().until(() -> trackedService.bundleInfos.size() == 1);
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

			System.out.println("check if we picked it all up correctly");
			await().until(() -> state.trackedServices.get(IF.class) != null);
			TrackedService ts = state.trackedServices.get(IF.class);
			assertThat(ts).isNotNull();
			assertThat(ts.satisfied()
				.size()).isEqualTo(0);
			assertThat(ts.bundleInfos.size()).isEqualTo(1);
			assertThat(ts.actualTypes.size()).isEqualTo(0);

			System.out.println("Register 2 different components that share the same actual type");
			Bundle a = lp.component(AggReq1.class);

			assertThat(ts.actualTypes.size()).isEqualTo(1);

			ActualType atr = ts.actualTypes.get(AggIF1.class);
			assertThat(atr).isNotNull();
			assertThat(atr.clients).isEqualTo(1);

			Bundle b = lp.component(AggReq2.class);
			assertThat(ts.actualTypes.size()).isEqualTo(1);

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
			assertThat(ts.bundleInfos.size()).isEqualTo(1);
			assertThat(atr.clients).isEqualTo(0);
			assertThat(atr.satisfied).isTrue();
			await().until(() -> atr.reg == null);

			b1.uninstall();

			await().until(() -> state.trackedServices.get(IF.class) == null);
			state.close();
		}
	}

	interface AggIF3 extends Aggregate<IF> {}

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
			assertThat(ts.satisfied()
				.size()).isEqualTo(0);
			assertThat(ts.bundleInfos.size()).isEqualTo(1);
			assertThat(ts.actualTypes.size()).isEqualTo(2);

			ActualType atr1 = ts.actualTypes.get(AggIF1.class);
			ActualType atr3 = ts.actualTypes.get(AggIF3.class);

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

			assertThat(ts.bundleInfos.size()).isEqualTo(1);
			await().until(() -> atr3.reg == null);

			b1.uninstall();

			await().until(() -> state.trackedServices.get(IF.class) == null);
			state.close();
		}
	}

	@Test
	public void testActualTypeOverride() throws Exception {
		String override = AggregateConstants.PREFIX_TO_OVERRIDE + AggIF1.class.getName();
		try (Launchpad lp = builder.create()) {
			System.setProperty(override, "2");
			AggregateState state = new AggregateState(lp.getBundleContext());
			ServiceRegistration<AggregateState> register = lp.register(AggregateState.class, state);
			lp.start();
			System.out.println(
				"Register a disabled component so we can check it picks up offers from prior registered bundles");
			Bundle falsePromise = lp.component(A1.class);
			Bundle empty = lp.bundle()
				.start();

			System.out.println("register a requirer");
			Bundle req = lp.component(AggReq1.class);

			TrackedService ts = state.trackedServices.get(IF.class);
			assertThat(ts.bundleInfos.size()).isEqualTo(1);

			System.out.println("verify we have picked up the global adjust");

			assertThat(ts.bundleInfos.size()).isEqualTo(1);
			assertThat(ts.override).isEqualTo(-1);
			assertThat(ts.satisfied()
				.size()).isEqualTo(0);
			assertThat(ts.actualTypes).hasSize(1);
			ActualType atr = ts.actualTypes.get(AggIF1.class);
			assertThat(atr).isNotNull();
			assertThat(atr.localOverride).isEqualTo(2);
			assertThat(atr.isSatisfied()).isFalse();

			System.out.println("register 2 services, should satisfy");
			ServiceRegistration<IF> s1 = falsePromise.getBundleContext()
				.registerService(IF.class, new A1(), null);
			ServiceRegistration<IF> s2 = empty.getBundleContext()
				.registerService(IF.class, new A1(), null);

			await().until(() -> ts.satisfied()
				.size() == 2);
			assertThat(atr.isSatisfied()).isTrue();

			ServiceRegistration<IF> s3 = lp.register(IF.class, new A1());
			await().until(() -> ts.satisfied()
				.size() == 3);
			assertThat(atr.isSatisfied()).isTrue();
			await().until(() -> atr.reg != null);

			state.close();

		} finally {
			System.getProperties()
				.remove(override);
		}

	}

	@Test
	public void testServiceTypeTypeOverride() throws Exception {
		String override = AggregateConstants.PREFIX_TO_OVERRIDE + IF.class.getName();
		try (Launchpad lp = builder.create()) {
			System.setProperty(override, "2");
			AggregateState state = new AggregateState(lp.getBundleContext());
			ServiceRegistration<AggregateState> register = lp.register(AggregateState.class, state);
			lp.start();

			System.out.println("register a requirer");
			Bundle req = lp.component(AggReq1.class);

			TrackedService ts = state.trackedServices.get(IF.class);
			assertThat(ts).isNotNull();
			assertThat(ts.bundleInfos.size()).isEqualTo(0);
			assertThat(ts.satisfied()
				.size()).isEqualTo(0);
			assertThat(ts.registeredServices).isEqualTo(0);
			assertThat(ts.override).isEqualTo(2);

			ActualType atr = ts.actualTypes.get(AggIF1.class);
			assertThat(atr).isNotNull();

			assertThat(atr.localOverride).isEqualTo(-1);
			assertThat(atr.isSatisfied()).isFalse();

			lp.register(IF.class, new A1());
			assertThat(ts.registeredServices).isEqualTo(1);
			assertThat(ts.satisfied()
				.size()).isEqualTo(1);
			assertThat(atr.isSatisfied()).isFalse();

			lp.register(IF.class, new A1());
			assertThat(ts.registeredServices).isEqualTo(2);
			assertThat(ts.satisfied()
				.size()).isEqualTo(1);
			assertThat(atr.isSatisfied()).isTrue();

			state.close();

		} finally {
			System.getProperties()
				.remove(override);
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
			assertThat(ts.bundleInfos.size()).isEqualTo(2);

			BundleInfo falsePromiseInfo = ts.bundleInfos.get(falsePromise);
			assertThat(falsePromiseInfo.actual).isEqualTo(0);
			assertThat(falsePromiseInfo.max).isEqualTo(1);

			BundleInfo aggrPromise3Info = ts.bundleInfos.get(aggrPromise3);
			assertThat(aggrPromise3Info.actual).isEqualTo(0);
			assertThat(aggrPromise3Info.max).isEqualTo(3);

			assertThat(ts.actualTypes).isEmpty();

			System.out.println("add an actualType");
			Bundle aggIF1 = lp.bundle()
				.addResource(AggIF1.class)
				.header(AggregateConstants.AGGREGATE_OVERRIDE,
					IF.class.getName() + ";" + AggregateConstants.AGGREGATE_OVERRIDE_ACTUAL_ATTR + "=\""
						+ AggIF1.class.getName() + "," + AggIF1.class.getName() + "\"")
				.start();

			assertThat(ts.bundleInfos.size()).isEqualTo(2);
			assertThat(ts.actualTypes.size()).isEqualTo(1);

			ActualType atr = ts.actualTypes.get(AggIF1.class);
			assertThat(atr.clients).isEqualTo(2);

			aggIF1.stop();
			assertThat(atr.clients).isEqualTo(0);

			state.close();

		}

	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testBundleCanOnlyRegisterSingleService() throws Exception {
		try (Launchpad lp = builder.create()) {
			AggregateState state = new AggregateState(lp.getBundleContext());
			ServiceRegistration<AggregateState> register = lp.register(AggregateState.class, state);
			lp.start();
			System.out.println(
				"Register a disabled component so we can check it picks up offers from prior registered bundles");

			Bundle p1 = lp.component(A1.class);

			TrackedService ts = state.trackedServices.get(IF.class);
			assertThat(ts.bundleInfos.size()).isEqualTo(1);
			assertThat(ts.actualTypes).isEmpty();

			Bundle e1 = lp.bundle()
				.start();
			Bundle e2 = lp.bundle()
				.start();

			Bundle r = lp.component(AggReq1.class);
			assertThat(ts.actualTypes.size()).isEqualTo(1);

			ActualType at = ts.actualTypes.get(AggIF1.class);

			List<ServiceRegistration<?>> regs = new ArrayList<>();
			for (int i = 0; i < 100; i++) {
				ServiceRegistration<IF> registerService = p1.getBundleContext()
					.registerService(IF.class, new A1(), null);
				regs.add(registerService);
			}
			assertThat(at.isSatisfied()).isTrue();
			ServiceRegistration<?> remove = regs.remove(0);
			remove.unregister();
			assertThat(at.isSatisfied()).isFalse();

			ServiceRegistration<IF> registerService = p1.getBundleContext()
				.registerService(IF.class, new A1(), null);

			assertThat(at.isSatisfied()).isTrue();

			state.close();

		}

	}

	@AggregateSettings(override = 5)
	interface AggIF4 extends Aggregate<IF> {}

	final static AtomicReference<AggReq4> ref4 = new AtomicReference<>();

	@Component
	public static class AggReq4 {

		{
			ref4.set(this);
		}

		@Reference
		AggIF4 iff;
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testOverrideWithAnnotation() throws Exception {
		try (Launchpad lp = builder.create()) {
			AggregateState state = new AggregateState(lp.getBundleContext());
			ServiceRegistration<AggregateState> register = lp.register(AggregateState.class, state);
			lp.start();

			System.out.println("Promise 1 bundle");
			Bundle p1 = lp.component(A1.class);
			Bundle p2 = lp.component(A1.class);

			TrackedService ts = state.trackedServices.get(IF.class);
			assertThat(ts.bundleInfos.size()).isEqualTo(2);
			assertThat(ts.satisfied()
				.size()).isEqualTo(0);
			assertThat(ts.registeredServices).isEqualTo(0);
			assertThat(ts.actualTypes).isEmpty();

			Bundle r1 = lp.component(AggReq1.class);
			Bundle r4 = lp.component(AggReq4.class);

			assertThat(ts.actualTypes.size()).isEqualTo(2);
			assertThat(ts.bundleInfos.size()).isEqualTo(2);
			assertThat(ts.satisfied()
				.size()).isEqualTo(0);
			assertThat(ts.registeredServices).isEqualTo(0);

			ActualType atr1 = ts.actualTypes.get(AggIF1.class);
			ActualType atr4 = ts.actualTypes.get(AggIF4.class);

			assertThat(atr1.clients).isEqualTo(1);
			assertThat(atr4.clients).isEqualTo(1);

			assertThat(atr1.localOverride).isEqualTo(-1);
			assertThat(atr1.isSatisfied()).isFalse();
			assertThat(atr1.localOverride).isEqualTo(-1);
			assertThat(atr1.isSatisfied()).isFalse();

			ServiceRegistration reg1 = p1.getBundleContext()
				.registerService(IF.class, new A1(), null);

			assertThat(ts.bundleInfos.size()).isEqualTo(2);
			assertThat(ts.satisfied()
				.size()).isEqualTo(1);
			assertThat(ts.registeredServices).isEqualTo(1);

			assertThat(atr1.isSatisfied()).isFalse();
			assertThat(atr4.isSatisfied()).isFalse();

			ServiceRegistration reg2 = p1.getBundleContext()
				.registerService(IF.class, new A1(), null);

			assertThat(ts.bundleInfos.size()).isEqualTo(2);
			assertThat(ts.satisfied()
				.size()).isEqualTo(1);
			assertThat(ts.registeredServices).isEqualTo(2);

			assertThat(atr1.isSatisfied()).isFalse();
			assertThat(atr4.isSatisfied()).isFalse();

			ServiceRegistration reg3 = p2.getBundleContext()
				.registerService(IF.class, new A1(), null);

			assertThat(ts.bundleInfos.size()).isEqualTo(2);
			assertThat(ts.satisfied()
				.size()).isEqualTo(2);
			assertThat(ts.registeredServices).isEqualTo(3);

			assertThat(atr1.isSatisfied()).isTrue();
			assertThat(atr4.isSatisfied()).isFalse();

			ServiceRegistration reg4 = p2.getBundleContext()
				.registerService(IF.class, new A1(), null);
			assertThat(ts.satisfied()
				.size()).isEqualTo(2);
			assertThat(ts.registeredServices).isEqualTo(4);
			assertThat(atr1.isSatisfied()).isTrue();
			assertThat(atr4.isSatisfied()).isFalse();

			ServiceRegistration reg5 = p2.getBundleContext()
				.registerService(IF.class, new A1(), null);
			assertThat(ts.satisfied()
				.size()).isEqualTo(2);
			assertThat(ts.registeredServices).isEqualTo(5);
			assertThat(atr1.isSatisfied()).isTrue();
			assertThat(atr4.isSatisfied()).isTrue();

			ServiceRegistration reg6 = p2.getBundleContext()
				.registerService(IF.class, new A1(), null);
			assertThat(ts.satisfied()
				.size()).isEqualTo(2);
			assertThat(ts.registeredServices).isEqualTo(6);
			assertThat(atr1.isSatisfied()).isTrue();
			assertThat(atr4.isSatisfied()).isTrue();

			ServiceRegistration reg7 = p2.getBundleContext()
				.registerService(IF.class, new A1(), null);
			assertThat(ts.satisfied()
				.size()).isEqualTo(2);
			assertThat(ts.registeredServices).isEqualTo(7);
			assertThat(atr1.isSatisfied()).isTrue();
			assertThat(atr4.isSatisfied()).isTrue();

			System.out.println(
				"unregister reg1 from p1. Since we registered 2(max), this becomes unsatisfied for bundles but override is happy");
			reg1.unregister();
			assertThat(ts.satisfied()
				.size()).isEqualTo(1);
			assertThat(ts.registeredServices).isEqualTo(6);
			assertThat(atr1.isSatisfied()).isFalse();
			assertThat(atr4.isSatisfied()).isTrue();

			System.out.println(
				"unregister reg2 from p1. Since we registered 2(max), this remains unsatisfied for bundles but override stays happy");
			reg2.unregister();
			assertThat(ts.satisfied()
				.size()).isEqualTo(1);
			assertThat(ts.satisfied()
				.size()).isEqualTo(1);
			assertThat(ts.registeredServices).isEqualTo(5);
			assertThat(atr1.isSatisfied()).isFalse();
			assertThat(atr4.isSatisfied()).isTrue();

			reg7.unregister();

			assertThat(ts.registeredServices).isEqualTo(4);
			assertThat(atr1.isSatisfied()).isFalse();
			assertThat(atr4.isSatisfied()).isFalse();
			state.close();

		}

	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testLocalOverrideWithSystemProperty() throws Exception {
		try (Launchpad lp = builder.create()) {
			AggregateState state = new AggregateState(lp.getBundleContext());
			ServiceRegistration<AggregateState> register = lp.register(AggregateState.class, state);
			lp.start();

			System.out.println("Promise 1 bundle");
			Bundle p1 = lp.component(A1.class);
			Bundle p2 = lp.component(A1.class);

			TrackedService ts = state.trackedServices.get(IF.class);
			assertThat(ts.bundleInfos.size()).isEqualTo(2);
			assertThat(ts.satisfied()
				.size()).isEqualTo(0);
			assertThat(ts.registeredServices).isEqualTo(0);
			assertThat(ts.actualTypes).isEmpty();

			Bundle r1 = lp.component(AggReq1.class);
			Bundle r4 = lp.component(AggReq4.class);

			assertThat(ts.actualTypes.size()).isEqualTo(2);
			assertThat(ts.bundleInfos.size()).isEqualTo(2);
			assertThat(ts.satisfied()
				.size()).isEqualTo(0);
			assertThat(ts.registeredServices).isEqualTo(0);

			ActualType atr1 = ts.actualTypes.get(AggIF1.class);
			ActualType atr4 = ts.actualTypes.get(AggIF4.class);

			assertThat(atr1.clients).isEqualTo(1);
			assertThat(atr4.clients).isEqualTo(1);

			assertThat(atr1.localOverride).isEqualTo(-1);
			assertThat(atr1.isSatisfied()).isFalse();
			assertThat(atr1.localOverride).isEqualTo(-1);
			assertThat(atr1.isSatisfied()).isFalse();

			ServiceRegistration reg1 = p1.getBundleContext()
				.registerService(IF.class, new A1(), null);

			assertThat(ts.bundleInfos.size()).isEqualTo(2);
			assertThat(ts.satisfied()
				.size()).isEqualTo(1);
			assertThat(ts.registeredServices).isEqualTo(1);

			assertThat(atr1.isSatisfied()).isFalse();
			assertThat(atr4.isSatisfied()).isFalse();

			ServiceRegistration reg2 = p1.getBundleContext()
				.registerService(IF.class, new A1(), null);

			assertThat(ts.bundleInfos.size()).isEqualTo(2);
			assertThat(ts.satisfied()
				.size()).isEqualTo(1);
			assertThat(ts.registeredServices).isEqualTo(2);

			assertThat(atr1.isSatisfied()).isFalse();
			assertThat(atr4.isSatisfied()).isFalse();

			ServiceRegistration reg3 = p2.getBundleContext()
				.registerService(IF.class, new A1(), null);

			assertThat(ts.bundleInfos.size()).isEqualTo(2);
			assertThat(ts.satisfied()
				.size()).isEqualTo(2);
			assertThat(ts.registeredServices).isEqualTo(3);

			assertThat(atr1.isSatisfied()).isTrue();
			assertThat(atr4.isSatisfied()).isFalse();

			ServiceRegistration reg4 = p2.getBundleContext()
				.registerService(IF.class, new A1(), null);
			assertThat(ts.satisfied()
				.size()).isEqualTo(2);
			assertThat(ts.registeredServices).isEqualTo(4);
			assertThat(atr1.isSatisfied()).isTrue();
			assertThat(atr4.isSatisfied()).isFalse();

			ServiceRegistration reg5 = p2.getBundleContext()
				.registerService(IF.class, new A1(), null);
			assertThat(ts.satisfied()
				.size()).isEqualTo(2);
			assertThat(ts.registeredServices).isEqualTo(5);
			assertThat(atr1.isSatisfied()).isTrue();
			assertThat(atr4.isSatisfied()).isTrue();

			ServiceRegistration reg6 = p2.getBundleContext()
				.registerService(IF.class, new A1(), null);
			assertThat(ts.satisfied()
				.size()).isEqualTo(2);
			assertThat(ts.registeredServices).isEqualTo(6);
			assertThat(atr1.isSatisfied()).isTrue();
			assertThat(atr4.isSatisfied()).isTrue();

			ServiceRegistration reg7 = p2.getBundleContext()
				.registerService(IF.class, new A1(), null);
			assertThat(ts.satisfied()
				.size()).isEqualTo(2);
			assertThat(ts.registeredServices).isEqualTo(7);
			assertThat(atr1.isSatisfied()).isTrue();
			assertThat(atr4.isSatisfied()).isTrue();

			reg1.unregister();
			assertThat(ts.registeredServices).isEqualTo(6);
			assertThat(atr1.isSatisfied()).isFalse();
			assertThat(atr4.isSatisfied()).isTrue();

			reg2.unregister();
			assertThat(ts.satisfied()
				.size()).isEqualTo(1);
			assertThat(ts.satisfied()
				.size()).isEqualTo(1);
			assertThat(ts.registeredServices).isEqualTo(5);
			assertThat(atr1.isSatisfied()).isFalse();
			assertThat(atr4.isSatisfied()).isTrue();

			reg7.unregister();
			assertThat(ts.registeredServices).isEqualTo(4);
			assertThat(atr1.isSatisfied()).isFalse();
			assertThat(atr4.isSatisfied()).isFalse();
			state.close();

		}

	}
}
