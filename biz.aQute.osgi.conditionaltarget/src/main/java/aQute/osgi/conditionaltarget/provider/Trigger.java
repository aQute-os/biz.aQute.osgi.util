package aQute.osgi.conditionaltarget.provider;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implements a postponed trigger. If {@link #trigger()} is called, then the
 * callback {@link #trigger} is executed after some specified delay. If in the
 * mean time the {@link #trigger()} is called again, the task is further
 * postponed.
 */
public class Trigger implements Closeable {
	final Runnable					trigger;
	final long						idlePeriod;
	final ScheduledExecutorService	scheduler	= Executors.newScheduledThreadPool(0);

	private long					deadline;
	private ScheduledFuture<?>		schedule;

	/**
	 * Create a trigger with a callback and an idleperiod
	 * 
	 * @param r
	 *            the callback executed idlePeriod ms after last trigger
	 * @param idlePeriod
	 *            the idle period in ms
	 */
	public Trigger(Runnable r, long idlePeriod) {
		this.trigger = r;
		this.idlePeriod = idlePeriod;
	}

	/**
	 * Trigger, this will execute the callback {@link #idlePeriod} ms after this
	 * method or later when another {@link #trigger()} is called in the mean
	 * time.
	 */
	public synchronized void trigger() {
		deadline = System.currentTimeMillis() + idlePeriod;
		schedule();
	}

	private void schedule() {
		boolean run = false;
		synchronized (this) {
			if (schedule != null)
				schedule.cancel(false);

			long delay = deadline - System.currentTimeMillis();
			if (delay <= 10) {
				run = true;
			} else {
				schedule = scheduler.schedule(this::schedule, delay, TimeUnit.MILLISECONDS);
			}
		}
		if (run)
			trigger.run();
	}

	@Override
	public synchronized void close() throws IOException {
		scheduler.shutdownNow();
	}
}
