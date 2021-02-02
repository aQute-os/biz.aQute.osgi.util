package biz.aQute.scheduler.basic.provider;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import biz.aQute.scheduler.api.Scheduler;
import biz.aQute.scheduler.api.Task;

/**
 * Provides a provider for the Scheduler. This simple implementation does cancel
 * jobs when the bundle is service is ungotten.
 */
@Component(scope = ServiceScope.PROTOTYPE)
public class SchedulerImpl implements Executor, Scheduler {

	final Set<Task>			tasks	= Collections.synchronizedSet(new HashSet<>());
	final CentralScheduler	scheduler;

	class TaskImpl implements Runnable, Task {
		Runnable cancel;

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean cancel() {
			// TODO Auto-generated method stub
			return false;
		}
	}

	@Activate
	public SchedulerImpl(@Reference CentralScheduler s) {
		this.scheduler = s;
	}

	@Deactivate
	void deactivate() {
		tasks.forEach(Task::cancel);
	}

	@Override
	public Task periodic(Runnable runnable, long ms, String name) {
		TaskImpl task = createWrapper(runnable, name, false);
		ScheduledFuture<?> future = scheduler.scheduler.scheduleAtFixedRate(task, ms, ms, TimeUnit.MILLISECONDS);
		task.cancel = () -> future.cancel(true);
		tasks.add(task);
		return task;
	}

	@Override
	public Task after(Runnable runnable, long ms, String name) {
		TaskImpl task = createWrapper(runnable, name, false);
		ScheduledFuture<?> future = scheduler.scheduler.schedule(task, ms, TimeUnit.MILLISECONDS);
		task.cancel = () -> future.cancel(true);
		tasks.add(task);
		return task;
	}

	@Override
	public Task execute(Runnable runnable, String name) {
		TaskImpl task = createWrapper(runnable, name, false);
		scheduler.scheduler.execute(task);
		tasks.add(task);
		return task;
	}

	private TaskImpl createWrapper(Runnable runnable, String name, boolean manage) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Promise<T> submit(Callable<T> callable, String name) {
		Deferred<T> deferred = scheduler.factory.deferred();
		TaskImpl task = createWrapper(() -> {
			try {
				deferred.resolve(callable.call());
			} catch (Throwable e) {
				deferred.fail(e);
			}
		}, name, false);

		scheduler.scheduler.execute(task);
		tasks.add(task);
		return deferred.getPromise();
	}

	@Override
	public Task deamon(RunnableWithException r, boolean manage, String name) {
		
		return null;
	}

	@Override
	public void execute(Runnable command) {
		execute(command, Instant.now().toString());
	}

}
