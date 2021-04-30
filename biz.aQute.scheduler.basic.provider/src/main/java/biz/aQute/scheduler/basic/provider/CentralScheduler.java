package biz.aQute.scheduler.basic.provider;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.converter.Converter;
import biz.aQute.scheduler.api.CronJob;
import biz.aQute.scheduler.api.Task;
import biz.aQute.scheduler.basic.config.SchedulerConfig;
import biz.aQute.scheduler.basic.provider.SchedulerImpl.TaskImpl;

//@formatter:off
@Designate(
			ocd 				= 	SchedulerConfig.class,
			factory 			= 	false
)
@Component(
			service 			= 	CentralScheduler.class,
			scope 				= 	ServiceScope.SINGLETON,
			immediate 			= 	true,
			configurationPolicy = 	ConfigurationPolicy.OPTIONAL,
			name 				= 	SchedulerConfig.PID
)
//@formatter:on
public class CentralScheduler {
	final List<Cron>				crons			= new ArrayList<>();
	final static Logger				logger			= LoggerFactory.getLogger(SchedulerImpl.class);
	final ScheduledExecutorService	scheduler;
	final PromiseFactory			factory;
	Clock							clock			= Clock.systemDefaultZone();
	long							shutdownTimeout	= 5000;
	final SchedulerImpl				frameworkTasks	= new SchedulerImpl(this);

	@Activate
	public CentralScheduler(SchedulerConfig config) {
		scheduler = Executors.newScheduledThreadPool(50);
		factory = new PromiseFactory(scheduler);
		modified(config);
	}

	@Modified
	public void modified(SchedulerConfig config) {
		if (config != null) {
			String timeZoneString = config.timeZone();
			if (timeZoneString != null && !SchedulerConfig.SYSTEM_DEFAULT_TIMEZONE.equals(timeZoneString))
				try {
					if ("UTC".equals(timeZoneString)) {
						clock = Clock.systemUTC();
					} else {
						ZoneId zone = ZoneId.of(timeZoneString);
						clock = Clock.system(zone);
					}
				} catch (Exception e) {
					logger.error("Invalid configuration time zone {}", timeZoneString);
				}
		}
	}

	@Deactivate
	void deactivate() {
		frameworkTasks.deactivate();
		scheduler.shutdown();
		try {
			if (scheduler.awaitTermination(500, TimeUnit.MILLISECONDS))
				return;
			logger.info("waiting for scheduler to shutdown");
			scheduler.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS);
			if (!scheduler.isTerminated()) {
				logger.info("forcing shutdown");
				List<Runnable> shutdownNow = scheduler.shutdownNow();
				if (!shutdownNow.isEmpty())
					logger.warn("could not termninate {}", shutdownNow);
			}
		} catch (InterruptedException e) {
			Thread.currentThread()
					.interrupt();
			logger.info("terminated by interrupt");
		}
	}

	public <T> Promise<T> submit(Callable<T> callable, String name) {
		return factory.submit(() -> {
			String tname = Thread.currentThread()
					.getName();
			Thread.currentThread()
					.setName(name);
			try {
				return callable.call();
			} catch (Exception e) {
				logger.warn("submit {} failed with {}", name, e, e);
				throw e;
			} finally {
				Thread.currentThread()
						.setName(tname);
			}
		});
	}

	class Cron {

		CronJob	target;
		Task	schedule;

		Cron(CronJob target, String cronExpression, String name) throws Exception {
			this.target = target;
			this.schedule = frameworkTasks.schedule(target::run, cronExpression, name);
		}

		void close() throws IOException {
			schedule.cancel();
		}
	}

	void schedule(TaskImpl task, CronAdjuster cron, long delay) {
		synchronized (task) {
			if (task.canceled) {
				return;
			}
			ScheduledFuture<?> schedule = scheduler.schedule(() -> {
				task.run();
				schedule(task, cron, nextDelay(cron));
			}, delay, TimeUnit.MILLISECONDS);
			task.cancel = () -> schedule.cancel(true);
		}
	}

	long nextDelay(CronAdjuster cron) {
		ZonedDateTime now = ZonedDateTime.now(clock);
		ZonedDateTime next = now.with(cron);
		long delay = next.toInstant()
				.toEpochMilli() - System.currentTimeMillis();
		if (delay < 1)
			delay = 1;
		return delay;
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
