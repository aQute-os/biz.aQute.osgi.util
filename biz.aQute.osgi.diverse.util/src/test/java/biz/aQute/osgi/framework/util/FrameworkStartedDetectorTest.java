package biz.aQute.osgi.framework.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import biz.aQute.osgi.framework.util.FrameworkStartedDetector.Reason;

public class FrameworkStartedDetectorTest {
	static ExecutorService es = Executors.newCachedThreadPool();

	@AfterClass
	public static void after() throws InterruptedException {
		es.shutdownNow();
		boolean awaitTermination = es.awaitTermination(10, TimeUnit.SECONDS);
		assertThat(awaitTermination).isTrue();
	}
	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("test.bndrun");

	static long				sleep	= 1500;
	public static class Act implements BundleActivator {

		@Override
		public void start(BundleContext context) throws Exception {
			System.out.println("start " + context.getBundle());
			Thread.sleep(sleep);
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			System.out.println("stop " + context.getBundle());
		}

	}

	@Test
	public void testNormalStartupDetection() throws Exception {

		try (Launchpad lp = builder.nostart()
			.set(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "51")
			.create()) {

			FrameworkStartedDetector fsd = new FrameworkStartedDetector(lp.getBundleContext());
			CountDownLatch cl = new CountDownLatch(1);

			es.execute(() -> {
				System.out.println("started" + fsd.waitForStart(10_000_000));
				cl.countDown();
			});

			Bundle b1 = lp.bundle()
				.header("Aggregate", "foo=1")
				.bundleActivator(Act.class)
				.start();
			Bundle b2 = lp.bundle()
				.header("Aggregate", "foo=2")
				.bundleActivator(Act.class)
				.start();

			BundleStartLevel b2sl = b2.adapt(BundleStartLevel.class);
			b2sl.setStartLevel(50);

			lp.getFramework()
				.start();

			System.out.println("fw started");
			Awaitility.await()
				.until(() -> fsd.started.getCount() <= 0);
			System.out.println("fw started was detected");
			assertThat(b1.getState()).isEqualTo(Bundle.ACTIVE);
			System.out.println("closing down");
		}
		System.out.println("done");
	}

	@Test
	public void testStartupDetectionWhenAlreadyStartedFramework() throws Exception {
		System.out.println("testStartupDetectionWhenAlreadyStartedFramework");
		sleep = 100;
		try (Launchpad lp = builder.nostart()
			.set(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "1")
			.create()) {

			lp.getFramework()
				.start();

			System.out.println("fw started");

			lp.bundle()
				.header("Aggregate", "foo=1")
				.bundleActivator(Act.class)
				.start();

			FrameworkStartedDetector fsd = new FrameworkStartedDetector(lp.getBundleContext());
			CountDownLatch cl = new CountDownLatch(1);

			es.execute(() -> {
				fsd.waitForStart(150);
				System.out.println("started");
				cl.countDown();
			});

			Awaitility.await()
				.until(() -> fsd.getReason() != Reason.WAITING);

			assertThat(fsd.getReason()).isEqualTo(Reason.TIMEOUT);
		}
	}

	@Test
	public void testUsingStartlevels() throws Exception {
		System.out.println("testStartupDetectionWhenAlreadyStartedFramework");
		sleep = 100;
		try (Launchpad lp = builder.nostart()
			.set(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "100")
			.create()) {

			lp.getFramework()
				.start();

			System.out.println("fw started");

			lp.bundle()
				.header("Aggregate", "foo=1")
				.bundleActivator(Act.class)
				.start();

			FrameworkStartedDetector fsd = new FrameworkStartedDetector(lp.getBundleContext());
			CountDownLatch cl = new CountDownLatch(1);

			es.execute(() -> {
				fsd.waitForStart(150);
				System.out.println("started");
				cl.countDown();
			});

			Awaitility.await()
				.until(() -> fsd.getReason() != Reason.WAITING);
			assertThat(fsd.getReason()).isEqualTo(Reason.STARTLEVEL);
		}
	}

	@Test
	public void testInterrupt() throws Exception {
		System.out.println("interrupt");
		sleep = 100;
		try (Launchpad lp = builder.nostart()
			.set(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "1")
			.create()) {

			lp.getFramework()
				.start();

			FrameworkStartedDetector fsd = new FrameworkStartedDetector(lp.getBundleContext());
			CountDownLatch cl = new CountDownLatch(1);

			AtomicReference<Thread> thread = new AtomicReference<>();

			es.execute(() -> {
				thread.set(Thread.currentThread());
				fsd.waitForStart(10000);
				cl.countDown();
			});

			Thread.sleep(500);
			thread.get()
				.interrupt();

			Awaitility.await()
				.until(() -> fsd.getReason() != Reason.WAITING);
			assertThat(fsd.getReason()).isEqualTo(Reason.INTERRUPTED);
		}
	}
}
