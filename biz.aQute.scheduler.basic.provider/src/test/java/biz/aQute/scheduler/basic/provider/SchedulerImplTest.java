package biz.aQute.scheduler.basic.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Test;
import org.osgi.util.promise.Promise;

import biz.aQute.scheduler.api.Task;

public class SchedulerImplTest {
	private static final int	MAX_WAIT	= 10000;
	CentralScheduler			cs			= new CentralScheduler();
	SchedulerImpl				impl		= new SchedulerImpl(cs);

	@After
	public void after() {
		impl.deactivate();
		assertThat(impl.tasks).isEmpty();
		assertThat(cs.scheduler.shutdownNow()).isEmpty();
		cs.deactivate();
		assertThat(cs.scheduler.isShutdown());
	}

	@Test
	public void testAfter() throws InterruptedException {

		SchedulerImpl impl = new SchedulerImpl(cs);
		try {
			CountDownLatch cdl = new CountDownLatch(1);

			long start = System.currentTimeMillis();
			Task after = impl.after(cdl::countDown, 500, "after");

			assertThat(cdl.await(10000, TimeUnit.MILLISECONDS)).isTrue();
			System.out.println("after delay " + (System.currentTimeMillis() - start) + "ms");
			after.cancel();
		} finally {
			impl.deactivate();
		}
	}

	@Test
	public void testPeriodic() throws InterruptedException {
		try {
			CountDownLatch cdl = new CountDownLatch(5);

			Task schedule = impl.periodic(cdl::countDown, 100, "periodic");

			assertThat(cdl.await(MAX_WAIT, TimeUnit.MILLISECONDS)).isTrue();

			schedule.cancel();

		} finally {
			impl.deactivate();
		}
	}

	@Test
	public void testExecute() throws InterruptedException {
		AtomicReference<Thread> thread = new AtomicReference<>();
		CountDownLatch cdl = new CountDownLatch(1);

		try {
			impl.execute(() -> {
				try {
					thread.set(Thread.currentThread());
					cdl.await();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}, "execute");

			Awaitility.await()
					.until(() -> thread.get() != null);

			assertThat(thread.get()
					.getName()
					.equals("execute"));
			cdl.countDown();

		} finally {
			impl.deactivate();
		}
	}

	@Test
	public void testShutdown() throws InterruptedException {
		AtomicReference<Thread> thread = new AtomicReference<>();

		try {
			cs.shutdownTimeout = 100;

			impl.execute(() -> {
				try {
					thread.set(Thread.currentThread());
					System.out.println("waiting");
					Object o = new Object();
					synchronized (o) {
						o.wait();
					}
				} catch (InterruptedException e) {
					System.out.println("interrupted");
				}
			}, "execute");

			Awaitility.await()
					.until(() -> thread.get() != null);
			impl.deactivate();

		} finally {
			impl.deactivate();
		}
	}

	@Test
	public void testSubmit() throws InterruptedException, InvocationTargetException {
		try {
			Promise<Integer> submit = impl.submit(() -> 5, "five");
			assertThat(submit.getValue()).isEqualTo(5);

			submit = impl.submit(() -> {
				throw new RuntimeException();
			}, "five");
			assertThat(submit.getFailure()).isInstanceOf(RuntimeException.class);
		} finally {
			impl.deactivate();
		}
	}
}
