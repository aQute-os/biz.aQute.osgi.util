package biz.aQute.scheduler.basic.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.promise.Deferred;

import biz.aQute.scheduler.api.CancelException;
import biz.aQute.scheduler.api.CancellablePromise;
import biz.aQute.scheduler.api.Scheduler;
import biz.aQute.scheduler.api.TimeoutException;

public class SchedulerTest {

	private ServiceReference<Scheduler> sr;
	private Scheduler si;

	BundleContext bc = FrameworkUtil.getBundle(SchedulerTest.class).getBundleContext();

	@Before
	public void before() throws InterruptedException {
	
		sr = bc.getServiceReference(Scheduler.class);

		si = bc.getService(sr);

	}

	@After
	public void after() {
		bc.ungetService(sr);

	}

	interface Foo {
		String foo();
	}

	@Test
	public void testCronReboot() throws Exception {
		final long now = System.currentTimeMillis();
		final Semaphore s = new Semaphore(0);
		si.schedule(Foo.class, (foo) -> {
			s.release();
		}, "@reboot");
		s.acquire(1);

		final long diff = System.currentTimeMillis() - now;
		assertTrue(diff < 100);
	}

	@Test
	public void testCron2() throws Exception {
		final long now = System.currentTimeMillis();
		final AtomicReference<String> ref = new AtomicReference<>();

		final Semaphore s = new Semaphore(0);
		si.schedule(Foo.class, (foo) -> {
			s.release();
			ref.set(foo.foo());
		}, "#\n" //
				+ "\n" //
				+ " foo = bar \n" //
				+ "# bla bla foo=foo\n" //
				+ "0/2 * * * * *");
		s.acquire(2);

		final long diff = (System.currentTimeMillis() - now + 500) / 1000;
		assertTrue(diff >= 3 && diff <= 4);
		assertEquals("bar", ref.get());
	}

	@Test
	public void testCancellableWithTimeout() throws InterruptedException, InvocationTargetException {
		final Deferred<Integer> d = new Deferred<>();
		final CancellablePromise<Integer> before = si.before(d.getPromise(), 100);
		before.cancel();
		assertEquals(CancelException.SINGLETON, before.getFailure());
	}

	@Test(expected = AssertionError.class)
	public void testResolveWithTimeout() throws InterruptedException, InvocationTargetException {
		final Deferred<Integer> d = new Deferred<>();
		final CancellablePromise<Integer> before = si.before(d.getPromise(), 100);
		d.resolve(3);
		assertTrue(before.isDone());
		assertEquals(Integer.valueOf(3), before.getValue());
	}

	@Test(expected = AssertionError.class)
	public void testFailureWithTimeout() throws InterruptedException, InvocationTargetException {
		final Deferred<Integer> d = new Deferred<>();

		final CancellablePromise<Integer> before = si.before(d.getPromise(), 100);

		final Exception e = new Exception();
		d.fail(e);

		assertTrue(before.isDone());
		assertEquals(e, before.getFailure());
	}

	@Test
	public void testTimeout() throws InterruptedException, InvocationTargetException {
		final Deferred<Integer> d = new Deferred<>();
		final CancellablePromise<Integer> before = si.before(d.getPromise(), 100);
		Thread.sleep(200);
		assertTrue(before.isDone());
		assertEquals(TimeoutException.SINGLETON, before.getFailure());
	}

	public void testNegative() throws InterruptedException {
		// Semaphore s = new Semaphore(0);
		// si.after(() -> {
		// s.release(1);
		// return null;
		// }, -100);
		// Thread.sleep(2);
		// assertEquals(1, s.availablePermits());
	}

	@Test
	public void testCron() throws Exception {
		final long now = System.currentTimeMillis();

		final Semaphore s = new Semaphore(0);
		si.schedule(() -> s.release(), "0/2 * * * * *");
		s.acquire(3);

		final long diff = (System.currentTimeMillis() - now + 500) / 1000;
		assertTrue(diff >= 5 && diff <= 6);
	}

	@Test
	public void testSchedule() throws Exception {
		// Don't run on AppVeyor, probably too slow
		//
		if (File.pathSeparatorChar == '\\') {
			return;
		}

		final long now = System.currentTimeMillis();

		final Semaphore s = new Semaphore(0);
		try (Closeable c = si.schedule(() -> s.release(), 100, 200, 300, 400)) {

			s.acquire(3);
			final long diff = System.currentTimeMillis() - now;
			assertEquals(6, (diff + 50) / 100);

			int n = s.availablePermits();
			Thread.sleep(3000);
			assertEquals(n + 7, s.availablePermits());
			c.close();
			n = s.availablePermits();
			Thread.sleep(3000);
			assertEquals(n, s.availablePermits());
		}
	}

	@Test
	public void testSimple() throws InterruptedException {
		final long now = System.currentTimeMillis();

		final Semaphore s = new Semaphore(0);

		si.after(10).then((p) -> {
			s.release(1);
			return null;
		});

		s.acquire();

		assertTrue(System.currentTimeMillis() - now > 9);
	}
}
