package biz.aQute.aggregate.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.runtime.ServiceComponentRuntime;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import biz.aQute.aggregate.api.Aggregate;
import biz.aQute.aggregate.provider.TrackedService.ActualTypeRegistration;

@SuppressWarnings("unused")
public class AggregateCounterTest {
	static LaunchpadBuilder					builder	= new LaunchpadBuilder().bndrun("test.bndrun");
	final static AtomicReference<AggReq1>	ref1	= new AtomicReference<>();

	@Before
	public void before() {
		ref1.set(null);
	}

	@Service
	ServiceComponentRuntime scr;

	interface IF {
	}

	@Aggregate(IF.class)
	interface AggIF1 {
	}

	@Component(immediate = true)
	public static class AggReq1 {
		final List<IF> list;

		@Activate
		public AggReq1(@Reference(policyOption = ReferencePolicyOption.GREEDY) List<IF> list) {
			this.list = list;
			ref1.set(this);
		}

		@Reference
		AggIF1 iff;
	}

	@Component(enabled = false)
	public static class A1 implements IF {

	}

	@Component
	public static class A2 implements IF {

	}

	@Test
	public void straightForwardTest() throws Exception {
		try (Launchpad lp = builder.create()) {

			System.out.println("Register an aggregate counter");

			AggregateCounter ac = new AggregateCounter(lp.getBundleContext());
			AggregateState aggregateState = ac.state;
			ServiceRegistration<AggregateCounter> register = lp.register(AggregateCounter.class, ac);

			System.out.println("Check that we've seen the bundles");
			assertThat(aggregateState.bundles).isNotEmpty();
			System.out.println("And there are no tracked services yet because nobody listened");
			assertThat(aggregateState.trackedServices).isEmpty();

			System.out.println(
					"Register a disabled component so we can check it picks up offers from prior registered bundles");
			Bundle a1bundle = lp.component(A1.class);

			System.out.println("Register our whiteboard, will create a tracked service");
			Bundle aggreq = lp.component(AggReq1.class);

			System.out.println("Wait until the listener has been seen");
			await().until(() -> !aggregateState.trackedServices.isEmpty());

			System.out.println("Get the TrackedService for the listener");
			TrackedService trackedAggIF = aggregateState.trackedServices.get(IF.class);
			assertThat(trackedAggIF).isNotNull();


			System.out.println("Check we have a client, not satisfied, requires 1, and no discovered");
			assertThat(trackedAggIF.registrations).hasSize(1);
			ActualTypeRegistration ar = trackedAggIF.registrations.get(AggIF1.class);
			assertThat(ar.satisfied).isFalse();
			assertThat(ar.localAdjust).isZero();
			assertThat(trackedAggIF.requiredByBundles).isEqualTo(1);
			assertThat(trackedAggIF.discovered).isEqualTo(0);

			System.out.println("Register a enabled component. require=2, discover=1");
			lp.component(A2.class);

			await().until(() -> trackedAggIF.discovered == 1);
			System.out.println("Still have only one client");
			assertThat(trackedAggIF.registrations.size()).isEqualTo(1);
			ActualTypeRegistration ar2 = trackedAggIF.registrations.get(AggIF1.class);
			assertThat(ar2.satisfied).isFalse();
			assertThat(trackedAggIF.requiredByBundles).isEqualTo(2);

			System.out.println("Register an IF and check if it is picked up");
			A1 a1 = new A1();
			ServiceRegistration<IF> ra1 = lp.register(IF.class, a1);
			await().until(() -> trackedAggIF.discovered == 2);
			await().until(() -> trackedAggIF.registrations.get(AggIF1.class).reg != null);
			await().until(() -> ref1.get() != null);

			AggReq1 aggReq1 = ref1.get();
			assertThat(aggReq1.iff).isNotNull();
			assertThat(aggReq1.list).isNotNull();
			assertThat(aggReq1.list).hasSize(2);

			System.out.println("Register an extra service, no change");
			ServiceRegistration<IF> ra1_1 = lp.register(IF.class, a1);
			await().until(() -> trackedAggIF.discovered == 3);
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF1.class) != null);

			System.out.println("Unregister the extra service");
			ra1_1.unregister();
			await().until(() -> trackedAggIF.discovered == 2);
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF1.class) != null);

			System.out.println("Remove the next service, will become dissatisfied");
			ra1.unregister();
			await().until(() -> trackedAggIF.discovered == 1);
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF1.class) == null);

			System.out.println("Remove the A1 bundle that offer 1 instance, so should become happy again");
			assertThat(trackedAggIF.requiredByBundles).isEqualTo(2);
			a1bundle.uninstall();
			await().until(() -> trackedAggIF.requiredByBundles == 1);

			await().until(() -> lp.getBundleContext().getServiceReference(AggIF1.class) != null);

			System.out.println("Uninstall the white board service, should clean up");
			aggreq.uninstall();

			System.out.println("Check that no services are tracked & the services is unregistered");
			await().until(() -> aggregateState.trackedServices.isEmpty());
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF1.class) == null);

			ac.close();

			register.unregister();
		}
	}

	@Component
	public static class AggReq2 {

		@Reference
		AggIF1 iff;
	}

	@Test
	public void test2ReqWithSameAggregateInterface() throws Exception {
		try (Launchpad lp = builder.create()) {
			AggregateCounter ac = new AggregateCounter(lp.getBundleContext());
			AggregateState aggregateState = ac.state;
			ServiceRegistration<AggregateCounter> register = lp.register(AggregateCounter.class, ac);

			System.out.println(
					"Register a disabled component so we can check it picks up offers from prior registered bundles");
			Bundle a1bundle = lp.component(A1.class);

			Bundle a = lp.component(AggReq1.class);
			Bundle b = lp.component(AggReq2.class);

			TrackedService trackIF = aggregateState.trackedServices.get(IF.class);
			assertThat(trackIF.requiredByBundles).isEqualTo(1);
			assertThat(trackIF.discovered).isZero();
			await().until(() -> trackIF.registrations.get(AggIF1.class).clients.size() == 2);

			a1bundle.getBundleContext().registerService(IF.class, new A1(), null);
			await().until(() -> trackIF.discovered == 1);
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF1.class) != null);
			ActualTypeRegistration typeRegistration = trackIF.registrations.get(AggIF1.class);
			assertThat(typeRegistration.reg).isNotNull();

			b.uninstall();
			await().until(() -> typeRegistration.clients.size() == 1);
			assertThat(typeRegistration.reg).isNotNull();
			assertThat(aggregateState.trackedServices.get(IF.class)).isNotNull();
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF1.class) != null);

			a.uninstall();
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF1.class) == null);
			await().until(() -> aggregateState.trackedServices.get(IF.class) == null);

			aggregateState.close();
		}
	}

	@Aggregate(IF.class)
	interface AggIF2 {
	}

	@Component
	public static class AggReq3 {

		@Reference
		AggIF2 iff;
	}

	@Test
	public void test2ReqWithDifferenceInterface() throws Exception {
		try (Launchpad lp = builder.create()) {
			AggregateCounter ac = new AggregateCounter(lp.getBundleContext());
			AggregateState aggregateState = ac.state;
			ServiceRegistration<AggregateCounter> register = lp.register(AggregateCounter.class, ac);

			System.out.println(
					"Register a disabled component so we can check it picks up offers from prior registered bundles");
			Bundle a1bundle = lp.component(A1.class);

			Bundle a = lp.component(AggReq1.class);
			Bundle b = lp.component(AggReq3.class);

			TrackedService trackIF = aggregateState.trackedServices.get(IF.class);
			assertThat(trackIF.requiredByBundles).isEqualTo(1);
			assertThat(trackIF.discovered).isZero();
			await().until(() -> trackIF.registrations.get(AggIF1.class).clients.size() == 1);
			await().until(() -> trackIF.registrations.get(AggIF2.class).clients.size() == 1);

			a1bundle.getBundleContext().registerService(IF.class, new A1(), null);
			await().until(() -> trackIF.discovered == 1);
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF1.class) != null);
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF2.class) != null);
			ActualTypeRegistration tr1 = trackIF.registrations.get(AggIF1.class);
			ActualTypeRegistration tr2 = trackIF.registrations.get(AggIF2.class);
			assertThat(tr1.reg).isNotNull();
			assertThat(tr2.reg).isNotNull();

			b.uninstall();

			await().until(() -> trackIF.registrations.get(AggIF2.class) == null);
			await().until(() -> trackIF.registrations.get(AggIF1.class) != null);
			assertThat(aggregateState.trackedServices.get(IF.class)).isNotNull();

			await().until(() -> lp.getBundleContext().getServiceReference(AggIF1.class) != null);
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF2.class) == null);

			a.uninstall();
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF1.class) == null);
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF2.class) == null);
			await().until(() -> aggregateState.trackedServices.get(IF.class) == null);

			aggregateState.close();
		}
	}

	public interface AggIF4 extends Iterable<IF> {
	}

	@Component
	public static class AggReq4 {

		@Reference
		AggIF4 iff;
	}

	@Test
	public void testClassBasedInsteadOfAnnotation() throws Exception {
		try (Launchpad lp = builder.create()) {
			AggregateCounter ac = new AggregateCounter(lp.getBundleContext());
			AggregateState aggregateState = ac.state;
			ServiceRegistration<AggregateCounter> register = lp.register(AggregateCounter.class, ac);

			System.out.println(
					"Register a disabled component so we can check it picks up offers from prior registered bundles");
			Bundle a1bundle = lp.component(A1.class);

			Bundle req = lp.component(AggReq4.class);

			TrackedService trackIF = aggregateState.trackedServices.get(IF.class);
			assertNotNull(trackIF);
			ActualTypeRegistration reg = trackIF.registrations.get(AggIF4.class);
			assertNotNull(reg);

			assertThat(reg.reg).isNull();

			a1bundle.getBundleContext().registerService(IF.class, new A1(), null);
			await().until(() -> lp.getBundleContext().getServiceReference(AggIF4.class) != null);

			ac.close();

		}

	}

	@Test
	public void testAdjust() throws Exception {
		try (Launchpad lp = builder.create()) {
			AggregateCounter ac = new AggregateCounter(lp.getBundleContext());
			AggregateState aggregateState = ac.state;
			ServiceRegistration<AggregateCounter> register = lp.register(AggregateCounter.class, ac);

			Bundle a1bundle = lp.component(A1.class);

			System.setProperty("aggregate." + IF.class.getName(), "10");
			Bundle req = lp.component(AggReq4.class);

			TrackedService trackIF = aggregateState.trackedServices.get(IF.class);
			assertThat(trackIF.adjust).isEqualTo(10);
			await().until(() -> trackIF.requiredByBundles == 1);

			await().until(() -> trackIF.registrations.get(AggIF4.class) != null);
			ActualTypeRegistration atr = trackIF.registrations.get(AggIF4.class);
			assertThat(atr).isNotNull();
			assertThat(atr.localAdjust).isZero();
			assertThat(trackIF.adjust).isEqualTo(10);

			ac.close();

		} finally {
			System.getProperties().remove("aggregate." + IF.class.getName());
		}

	}

	@Component
	public static class AggReq5 {

		@Reference(policyOption = ReferencePolicyOption.GREEDY)
		List<IF>	list;

		@Reference
		AggIF4		iff;
	}

	public void testStress() throws Exception {
		ExecutorService es = Executors.newCachedThreadPool();

		try (Launchpad lp = builder.create()) {
			AggregateCounter ac = new AggregateCounter(lp.getBundleContext());
			AggregateState aggregateState = ac.state;
			ServiceRegistration<AggregateCounter> register = lp.register(AggregateCounter.class, ac);

			Bundle a1bundle = lp.component(A1.class);
			Bundle a2bundle = lp.component(A1.class);
			Bundle a3bundle = lp.component(A1.class);

			Bundle req = lp.component(AggReq4.class);

			TrackedService trackIF = aggregateState.trackedServices.get(IF.class);
			await().until(() -> trackIF.requiredByBundles == 3);

			CountDownLatch cl = new CountDownLatch(100);
			Random random = new Random();
			for (int i = 0; i < 100; i++) {
				es.execute(() -> {
					for (int j = 0; j < 10000; j++)
						try {
							Thread.sleep(random.nextInt(5));
							ServiceRegistration<IF> reg = lp.getBundleContext().registerService(IF.class, new A1(),
									null);
							Thread.sleep(random.nextInt(10));
							reg.unregister();
							Thread.sleep(random.nextInt(50));

						} catch (InterruptedException e) {
							return;
						}

					cl.countDown();
				});
			}
			cl.await();
			ac.close();

		} finally {
			System.getProperties().remove("aggregate." + IF.class.getName());
		}

	}

	@Aggregate(value = IF.class, adjust = -1)
	interface AggIF6 {}

	@Component(immediate = true)
	public static class AggReq6 {
		@Reference
		AggIF6 iff;
	}

	@Test
	public void testLocalAdjust() throws Exception {
		try (Launchpad lp = builder.create()) {
			AggregateCounter ac = new AggregateCounter(lp.getBundleContext());
			AggregateState aggregateState = ac.state;
			ServiceRegistration<AggregateCounter> register = lp.register(AggregateCounter.class, ac);

			Bundle a1bundle = lp.component(A1.class);

			Bundle req = lp.component(AggReq6.class);

			TrackedService trackIF = aggregateState.trackedServices.get(IF.class);
			assertThat(trackIF).isNotNull();
			await().until(() -> trackIF.registrations.get(AggIF6.class) != null);

			ActualTypeRegistration atr = trackIF.registrations.get(AggIF6.class);
			assertThat(atr).isNotNull();
			assertThat(atr.localAdjust).isEqualTo(-1);
			assertThat(trackIF.adjust).isEqualTo(0);
			assertThat(atr.isSatisfied()).isTrue();
			await().until(() -> atr.reg != null);

			ac.close();

		}

	}

}
