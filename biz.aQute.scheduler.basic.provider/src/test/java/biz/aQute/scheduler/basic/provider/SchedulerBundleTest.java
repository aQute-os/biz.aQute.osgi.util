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

	static Semaphore		present	= new Semaphore(0);

	@Component(property = "cron=@reboot")
	public static class TestRebootService implements CronJob {

		@Override
		public void run() throws Exception {
			present.release();
		}

	}

	@Component(property = "cron=0/1 * * * * *")
	public static class TestCleanup implements CronJob {

		@Override
		public void run() throws Exception {
			present.release();
		}

	}

	@Test
	public void testAnnotation() throws Exception {
		try (Launchpad lp = builder.create()) {
			present.drainPermits();
			lp.bundle().addResource(TestRebootService.class).start();
			Optional<CronJob> service = lp.getService(CronJob.class);
			assertThat(service).isPresent();

			boolean found = present.tryAcquire(1, 5000, TimeUnit.MILLISECONDS);
			assertTrue(found);
		}
	}

	@Test
	public void testCleanedup() throws Exception {
		try (Launchpad lp = builder.create().inject(this)) {
			present.drainPermits();
			lp.report();

			Bundle bundle = lp.bundle().addResource(TestCleanup.class).start();
			Optional<CronJob> service = lp.getService(CronJob.class);
			assertThat(service).isPresent();

			boolean active = present.tryAcquire(2, 5000, TimeUnit.MILLISECONDS);
			assertTrue(active);

			bundle.stop();

			service = lp.getService(CronJob.class);
			assertThat(service).isNotPresent();

			Thread.sleep(1000);
			present.drainPermits();
			active = present.tryAcquire(1, 2000, TimeUnit.MILLISECONDS);
			assertFalse(active);

		}
	}
}
