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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.aQute.scheduler.api.Scheduler;
import biz.aQute.scheduler.api.Task;

/**
 * Provides a provider for the Scheduler. This simple implementation does cancel
 * jobs when the bundle is service is ungotten.
 */
@Component(scope = ServiceScope.PROTOTYPE)
public class SchedulerImpl implements Executor, Scheduler {
	final Logger			logger	= LoggerFactory.getLogger(SchedulerImpl.class);
	final Set<Task>			tasks	= Collections.synchronizedSet(new HashSet<>());
	final CentralScheduler	scheduler;
	final Object			lock	= new Object();

	class TaskImpl implements Runnable, Task {
		final RunnableWithException	runnable;
		final String				name;
		Runnable					cancel;
		Thread						thread;
		final boolean				manage;

		TaskImpl(RunnableWithException runnable, String name, boolean manage) {
			this.manage = manage;
			this.runnable = runnable::run;
			this.name = name;
		}

		TaskImpl(Runnable runnable, String name, boolean manage) {
			this((RunnableWithException) runnable::run, name, manage);
		}

		@Override
		public void run() {
			synchronized (lock) {
				thread = Thread.currentThread();
			}
			String old = thread.getName();
			try {
				boolean logged=false;
				while (!thread.isInterrupted())
					try {
						thread.setName(name);
						runnable.run();
					} catch (Exception e) {
						if (!manage) {
							return;
						}
						if (!logged) {
							logger.info("managed task {} failed with {}. Sleeping 1sec", name, e, e);
							logged = true;
						}
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							thread.interrupt();
							logger.info("managed task {} interrupted", name);
							return;
						}
					}
			} finally {
				thread.setName(old);
				synchronized (lock) {
					thread = null;
					tasks.remove(this);
				}
			}
		}

		@Override
		public boolean cancel() {
			if (cancel != null)
				cancel.run();
			synchronized (lock) {
				if (tasks.remove(this)) {
					if (thread == null) {
						return false;
					}
					thread.interrupt();
				} else
					return false;
			}
			return true;
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
		TaskImpl task = new TaskImpl(runnable, name, false);
		ScheduledFuture<?> future = scheduler.scheduler.scheduleAtFixedRate(task, ms, ms, TimeUnit.MILLISECONDS);
		task.cancel = () -> future.cancel(true);
		tasks.add(task);
		return task;
	}

	@Override
	public Task after(Runnable runnable, long ms, String name) {
		TaskImpl task = new TaskImpl(runnable, name, false);
		ScheduledFuture<?> future = scheduler.scheduler.schedule(task, ms, TimeUnit.MILLISECONDS);
		task.cancel = () -> future.cancel(true);
		tasks.add(task);
		return task;
	}

	@Override
	public Task execute(Runnable runnable, String name) {
		TaskImpl task = new TaskImpl(runnable, name, false);
		scheduler.scheduler.execute(task);
		tasks.add(task);
		return task;
	}

	@Override
	public <T> Promise<T> submit(Callable<T> callable, String name) {
		Deferred<T> deferred = scheduler.factory.deferred();
		TaskImpl task = new TaskImpl((Runnable) () -> {
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
		TaskImpl task = new TaskImpl(r, name, manage);
		Thread thread = new Thread(task, name);
		thread.start();
		return task;
	}

	@Override
	public void execute(Runnable command) {
		execute(command, Instant.now().toString());
	}

}
