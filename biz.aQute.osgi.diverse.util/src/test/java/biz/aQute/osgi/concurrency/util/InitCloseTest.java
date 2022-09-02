package biz.aQute.osgi.concurrency.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.AfterClass;
import org.junit.Test;

import aQute.lib.exceptions.RunnableWithException;
import biz.aQute.osgi.concurrency.util.InitClose.State;

public class InitCloseTest {
	static ExecutorService es = Executors.newCachedThreadPool();

	@AfterClass
	public static void after() throws InterruptedException {
		es.shutdownNow();
		boolean awaitTermination = es.awaitTermination(10, TimeUnit.SECONDS);
		assertThat(awaitTermination).isTrue();
	}

	static class Tester implements Supplier<AutoCloseable> {
		Semaphore			init	= new Semaphore(0);
		Semaphore			close	= new Semaphore(0);
		volatile Thread		thread;
		volatile boolean	inclose	= false;
		volatile boolean	interrupted	= false;
		volatile boolean	closed		= false;

		@Override
		public AutoCloseable get() {
			this.thread = Thread.currentThread();
			acquire(init);
			this.thread = null;
			return () -> {
				inclose = true;
				acquire(close);
				inclose = false;
				closed = true;
			};
		}

		private void acquire(Semaphore s) {
			try {
				s.acquire();
			} catch (InterruptedException e) {
				interrupted = true;
				return;
			} finally {
				inclose = false;
			}
		}

	}

	@Test
	public void testAsync() throws Exception {
		Tester tester = new Tester();
		InitClose ic = new InitClose(tester, false);
		es.execute(ic);

		await()
			.until(() -> tester.thread != null);

		assertThat(ic.state).isEqualTo(State.BUSY);
		assertThat(tester.inclose).isFalse();

		tester.init.release(1);
		await().until(() -> tester.thread == null);

		assertThat(ic.state).isEqualTo(State.ACTIVE);

		es.execute(wrap(ic::close));
		await()
			.until(() -> tester.inclose == true);

		await()
			.until(() -> ic.state == State.CLOSED);


		assertThat(tester.inclose).isTrue();

		tester.close.release();
		await().until(() -> tester.inclose == false);

	}

	@Test
	public void testSync() throws Exception {
		Tester tester = new Tester();
		InitClose ic = new InitClose(tester, true);
		es.execute(ic);

		await().until(() -> tester.thread != null);

		assertThat(ic.state).isEqualTo(State.BUSY);
		assertThat(tester.inclose).isFalse();

		tester.init.release(1);
		await().until(() -> tester.thread == null);

		assertThat(ic.state).isEqualTo(State.ACTIVE);

		es.execute(wrap(ic::close));
		await().until(() -> tester.inclose == true);

		await().until(() -> ic.state == State.SYNCING_CLOSE);

		assertThat(tester.inclose).isTrue();
		tester.close.release();

		await().until(() -> tester.inclose == false);

		await().until(() -> ic.state == State.CLOSED);
	}

	@Test
	public void testAsyncWithCloseBeforeActive() throws Exception {
		Tester tester = new Tester();
		InitClose ic = new InitClose(tester, false);
		es.execute(ic);

		await().until(() -> ic.state == State.BUSY);

		es.execute(wrap(ic::close));

		await().until(() -> ic.state == State.CLOSED);
		await().until(() -> tester.interrupted);

		await().until(() -> tester.inclose == true);
		tester.close.release();
		await().until(() -> tester.inclose == false);

	}

	@Test
	public void testSyncWithCloseBeforeActive() throws Exception {
		Tester tester = new Tester();
		InitClose ic = new InitClose(tester, true);
		es.execute(ic);

		await().until(() -> ic.state == State.BUSY);

		es.execute(wrap(ic::close));

		await().until(() -> ic.state == State.SYNCING_CLOSE);
		await().until(() -> tester.interrupted);

		await().until(() -> tester.inclose == true);
		tester.close.release();
		await().until(() -> tester.inclose == false);
		await().until(() -> ic.state == State.CLOSED);
		await().until(() -> tester.closed);

	}

	private Runnable wrap(RunnableWithException t) {
		return () -> {
			t.ignoreException()
				.run();
		};
	}

}
