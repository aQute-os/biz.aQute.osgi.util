package biz.aQute.scheduler.basic.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import biz.aQute.scheduler.api.CronJob;

public class SchedulerBundleTest {
	static LaunchpadBuilder	builder	= new LaunchpadBuilder().bndrun("test.bndrun");


	@Component(property = "cron=@reboot")
	public static class TestRebootService implements CronJob {
		static Semaphore		present	= new Semaphore(0);

		@Override
		public void run() throws Exception {
			present.release();
		}

	}

	@Component(property = "cron=0/1 * * * * *")
	public static class TestCleanup implements CronJob {
		static Semaphore		present	= new Semaphore(0);

		@Override
		public void run() throws Exception {
			present.release();
		}

	}

	@Test
	public void testAnnotation() throws Exception {
		try (Launchpad lp = builder.create()) {
			TestRebootService.present.drainPermits();
			lp.bundle().addResource(TestRebootService.class).start();
			Optional<CronJob> service = lp.getService(CronJob.class);
			assertThat(service).isPresent();

			boolean found = TestRebootService.present.tryAcquire(1, 5000, TimeUnit.MILLISECONDS);
			assertTrue(found);
		}
	}

	@Test
	public void testCleanedup() throws Exception {
		try (Launchpad lp = builder.create().inject(this)) {
			System.out.println("clean up permits");
			TestCleanup.present.drainPermits();
			System.out.println("report the status");
			lp.report();

			System.out.println("create a component TestCleanup in a new bundle and runs once per second");
			Bundle bundle = lp.bundle().addResource(TestCleanup.class).start();
			Optional<CronJob> service = lp.getService(CronJob.class);
			assertThat(service).isPresent();

			System.out.println("wait for the component to do its work");
			boolean active = TestCleanup.present.tryAcquire(1, 5000, TimeUnit.MILLISECONDS);
			assertTrue(active);
			
			System.out.println("stop the bundle");
			bundle.stop();
			TestCleanup.present.drainPermits();

			System.out.println("make sure the CronJob is no longer there");
			service = lp.getService(CronJob.class);
			assertThat(service).isNotPresent();

			System.out.println("Wait for 3 seconds to see if the cron job is activated");
			active = TestCleanup.present.tryAcquire(1, 3000, TimeUnit.MILLISECONDS);
			assertFalse(active);

		}
	}
}
