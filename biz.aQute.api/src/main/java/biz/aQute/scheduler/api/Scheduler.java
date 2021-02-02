package biz.aQute.scheduler.api;

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
	 * @param name The name of the task
	 * @param runnable the task to run
	 */
	Task periodic(Runnable runnable, long ms, String name);

	/**
	 * Schedule a runnable to run after a certain time. The schedule can be
	 * canceled by the returned task if it was not yet canceled.
	 *
	 * @param name The name of the task
	 * @param runnable the task to run
	 */
	Task after(Runnable runnable, long ms, String name);

	/**
	 * Executes a task in the background, intended for short term tasks
	 *
	 * @param name The name of the task
	 * @param runnable the task to run
	 */
	Task execute(Runnable runnable, String name);

	/**
	 * Submit a task and return a promise to the answer
	 */

	<T> Promise<T> submit(Callable<T> callable, String name);
	
	/**
	 * Execute long running task and optionally restart when
	 * exceptions are thrown.
	 * 
	 * @param r The body
	 * @param manage restart when it fails 
	 * 
	 */
	Task deamon(RunnableWithException r, boolean manage, String name);
}
