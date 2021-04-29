package biz.aQute.scheduler.basic.provider;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.converter.Converter;
import biz.aQute.scheduler.api.CronJob;
import biz.aQute.scheduler.api.Scheduler;
import biz.aQute.scheduler.api.Task;

/**
 * Provides a provider for the Scheduler. This simple implementation does cancel
 * jobs when the bundle is service is ungotten.
 */
@Component(scope = ServiceScope.PROTOTYPE)
public class SchedulerImpl implements Executor, Scheduler {
	final List<Cron>		crons	= new ArrayList<>();
	final Logger			logger	= LoggerFactory.getLogger(SchedulerImpl.class);
	final Set<Task>			tasks	= Collections.synchronizedSet(new HashSet<>());
	final CentralScheduler	scheduler;
	final Object			lock	= new Object();
	Clock					clock	= Clock.systemDefaultZone();

	class TaskImpl implements Runnable, Task {
		final RunnableWithException	runnable;
		final String				name;
		Runnable					cancel;
		Thread						thread;
		final boolean				manage;
		final boolean				repeat;
		public boolean				canceled;

		TaskImpl(RunnableWithException runnable, String name, boolean manage, boolean repeat) {
			this.manage = manage;
			this.repeat = repeat;
			this.runnable = runnable::run;
			this.name = name;
		}

		TaskImpl(Runnable runnable, String name, boolean manage) {
			this((RunnableWithException) runnable::run, name, manage, false);
		}

		@Override
		public void run() {
			synchronized (lock) {
				thread = Thread.currentThread();
			}
			String old = thread.getName();
			try {
				boolean logged = false;
				while (!thread.isInterrupted())
					try {
						thread.setName(name);
						runnable.run();
						if (!repeat) {
							return;
						}
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
						if (!repeat) {
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
				canceled = true;
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
		TaskImpl task = new TaskImpl(r, name, manage, false);
		Thread thread = new Thread(task, name);
		thread.start();
		return task;
	}

	@Override
	public void execute(Runnable command) {
		execute(command, Instant.now().toString());
	}

	@Override
	public Task schedule(RunnableWithException job, String cronExpression, String name) throws Exception {
		TaskImpl task = new TaskImpl(job, name, false, false);
		CronAdjuster cron = new CronAdjuster(cronExpression);

		schedule(task, cron, cron.isReboot() ? 1 : nextDelay(cron));
		tasks.add(task);
		return task;
	}

	private void schedule(TaskImpl task, CronAdjuster cron, long delay) {
		synchronized (task) {
			if (task.canceled) {
				return;
			}
			ScheduledFuture<?> schedule = scheduler.scheduler.schedule(() -> {
				System.out.println("tick");
				task.run();
				schedule(task, cron, nextDelay(cron));
			}, delay, TimeUnit.MILLISECONDS);
			task.cancel = () -> schedule.cancel(true);
		}
	}

	private long nextDelay(CronAdjuster cron) {
		ZonedDateTime now = ZonedDateTime.now(clock);
		ZonedDateTime next = now.with(cron);
		long delay = next.toInstant()
				.toEpochMilli() - System.currentTimeMillis();
		if (delay < 1)
			delay = 1;
		System.out.println("delay " + delay);
		return delay;
	}

	class Cron {

		CronJob	target;
		Task	schedule;

		Cron(CronJob target, String cronExpression, String name) throws Exception {
			this.target = target;
			this.schedule = schedule(target::run, cronExpression, name);
		}

		void close() throws IOException {
			schedule.cancel();
		}
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
	void addSchedule(CronJob s, Map<String, Object> map) throws Exception {
		String name = Converter.cnv(String.class, map.get(CronJob.NAME));
		String[] schedules = Converter.cnv(String[].class, map.get(CronJob.CRON));
		if (schedules == null || schedules.length == 0)
			return;

		if (name == null) {
			name = "unknown " + Instant.now();
		}

		synchronized (crons) {
			for (String schedule : schedules) {
				try {
					Cron cron = new Cron(s, schedule, name);
					crons.add(cron);
				} catch (Exception e) {
					logger.error("Invalid  cron expression " + schedule + " from " + map, e);
				}
			}
		}
	}

	void removeSchedule(CronJob s) {
		synchronized (crons) {
			for (Iterator<Cron> cron = crons.iterator(); cron.hasNext();) {
				Cron c = cron.next();
				if (c.target == s) {
					cron.remove();
					c.schedule.cancel();
				}
			}
		}
	}

}
