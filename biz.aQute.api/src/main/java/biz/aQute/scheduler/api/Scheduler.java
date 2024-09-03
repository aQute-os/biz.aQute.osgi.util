package biz.aQute.scheduler.api;

import java.time.Duration;
import java.time.temporal.TemporalAdjuster;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.util.promise.Promise;

/**
 * A simple scheduler for background tasks.
 */
@ProviderType
public interface Scheduler extends Executor {

	interface RunnableWithException {
		void run() throws Exception;
	}

	/**
	 * Schedule a runnable to run periodically at a fixed rate. The schedule can
	 * be canceled by the returned task.
	 *
	 * @param runnable
	 *            the task to run
	 * @param name
	 *            The name of the task
	 * @param fixedRateInMs
	 * 			  The fixed rate in milliseconds.
	 *
	 */
	Task periodic(Runnable runnable, long fixedRateInMs, String name);

	/**
	 * Schedule a runnable to run periodically at a fixed rate. The schedule can
	 * be canceled by the returned task.
	 *
	 * @param runnable
	 *            the task to run
	 * @param name
	 *            The name of the task
	 * @param fixedRate
	 * 			  The fixed rate in milliseconds.
	 *
	 */
	Task periodic(Runnable runnable, Duration fixedRate, String name);
	/**
	 * Schedule a runnable to run after a certain time. The schedule can be
	 * canceled by the returned task if it was not yet canceled.
	 *
	 * @param name
	 *            The name of the task
	 * @param runnable
	 *            the task to run
	 * @param afterMs
	 *            time in ms after the run is scheduled.
	 */
	Task after(Runnable runnable, long afterMs, String name);

	/**
	 * Schedule a runnable to run after a certain time. The schedule can be
	 * canceled by the returned task if it was not yet canceled.
	 *
	 * @param name
	 *            The name of the task
	 * @param runnable
	 *            the task to run
	 * @param after
	 *            duration after the run is scheduled.
	 */
	Task after(Runnable runnable, Duration after, String name);

	/**
	 * Executes a task in the background, intended for short term tasks
	 *
	 * @param name
	 *            The name of the task
	 * @param runnable
	 *            the task to run
	 */
	Task execute(Runnable runnable, String name);

	/**
	 * Submit a task and return a promise to the answer
	 */

	<T> Promise<T> submit(Callable<T> callable, String name);

	/**
	 * Execute long running task and optionally restart when exceptions are
	 * thrown.
	 *
	 * @param r
	 *            The body
	 * @param manage
	 *            restart when it fails
	 *
	 */
	Task deamon(RunnableWithException r, boolean manage, String name);

	/**
	 * Schedule a runnable to be executed for the give cron expression (See
	 * {@link CronExpression#expression()}). Every time when the cronExpression matches the current
	 * time, the runnable will be run. The method returns a closeable that can
	 * be used to stop scheduling. This variation does not take an environment
	 * object.
	 *
	 * @param r
	 *            The Runnable to run
	 * @param name
	 *            The name
	 * @param cronExpression
	 *            A Cron Expression
	 * @return A closeable to terminate the schedule
	 * @throws Exception
	 */
	Task schedule(RunnableWithException r, String cronExpression, String name) throws Exception;

	/**
	 * Return a {@link TemporalAdjuster} based on a Cron expression. You can use
	 * a Temporal Adjust to calculate the time beteween a date time and the next
	 * trigger point.
	 *
	 * <pre>
	 *  TemporalAdjuster cron = this.getCronAdjuster("@hourly");
	 * 	ZonedDateTime now = ZonedDateTime.now();
	 * 	ZonedDateTime next = now.with(cron);
	 * </pre>
	 *
	 * @param cronExpression a Cron expression as specified in {@link CronExpression#expression()}
	 * @return a Temporal Adjuster based on a cron expression
	 */
	TemporalAdjuster getCronAdjuster(String cronExpression);

}
