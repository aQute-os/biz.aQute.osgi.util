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

import org.osgi.annotation.bundle.Capability;
import org.osgi.namespace.implementation.ImplementationNamespace;
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
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.aQute.scheduler.api.Constants;
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
@Capability(
			namespace			= ImplementationNamespace.IMPLEMENTATION_NAMESPACE,
			name				= Constants.SPECIFICATION_NAME,
			version				= Constants.SPECIFICATION_VERSION
)
//@formatter:on
public class CentralScheduler {
	
	final List<ScheduledCronJob>	scheduledCrons	= new ArrayList<>();
	final static Logger				logger			= LoggerFactory.getLogger(SchedulerImpl.class);
	final static Converter			converter		= Converters.standardConverter();
	final ScheduledExecutorService	scheduler;
	final PromiseFactory			factory;
	Clock							clock			= Clock.systemDefaultZone();
	long							shutdownTimeoutSoft;
	long							shutdownTimeoutHard;
	final SchedulerImpl				frameworkTasks	= new SchedulerImpl(this);

	@Activate
	public CentralScheduler(SchedulerConfig config) {
		int corePoolSize = config == null ? SchedulerConfig.COREPOOLSIZE_DEFAUL : config.corePoolSize();
		shutdownTimeoutSoft = config == null ? SchedulerConfig.SHUTDOWNTIMEOUT_SOFT_DEFAUL : config.shutdownTimeoutSoft();
		shutdownTimeoutHard = config == null ? SchedulerConfig.SHUTDOWNTIMEOUT_HARD_DEFAUL : config.shutdownTimeoutHard();

		scheduler = Executors.newScheduledThreadPool(corePoolSize);
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
			if (scheduler.awaitTermination(shutdownTimeoutSoft, TimeUnit.MILLISECONDS)) {
				return;
			}
			logger.info("waiting for scheduler to shutdown");
			scheduler.awaitTermination(shutdownTimeoutHard, TimeUnit.MILLISECONDS);
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

	class ScheduledCronJob {

		CronJob	innerCrobJob;
		Task	schedule;

		ScheduledCronJob(CronJob cronJob, String cronExpression, String name) throws Exception {
			this.innerCrobJob = cronJob;
			this.schedule = frameworkTasks.schedule(this.innerCrobJob::run, cronExpression, name);
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
	void addSchedule(CronJob cronJob, Map<String, Object> map) throws Exception {
		String name = converter.convert(map.get(Constants.SERVICE_PROPERTY_CRONJOB_NAME)).to(String.class);
		String nameOld = converter.convert(map.get(CronJob.NAME)).to(String.class);

		String[] schedules = converter.convert(map.get(Constants.SERVICE_PROPERTY_CRONJOB_CRON)).to(String[].class);
		String[] schedulesOld = converter.convert(map.get(CronJob.CRON)).to(String[].class);

		if (schedules == null || schedules.length == 0) {
			if (schedulesOld == null || schedulesOld.length == 0) {
				return;
			}
			schedules = schedulesOld;
		}
		

		if (name == null) {
			if (nameOld == null) {
				name = Constants.CRONJOB_NAME_DEFAULT + " " + Instant.now();
			}else {
				name = nameOld;
			}
		}

		synchronized (scheduledCrons) {
			for (String schedule : schedules) {
				try {
					ScheduledCronJob cron = new ScheduledCronJob(cronJob, schedule, name);
					scheduledCrons.add(cron);
				} catch (Exception e) {
					logger.error("Invalid  cron expression " + schedule + " from " + map, e);
				}
			}
		}
	}

	void removeSchedule(CronJob cronJobToRemove) {
		synchronized (scheduledCrons) {
			for (Iterator<ScheduledCronJob> scheduledCronsIterator = scheduledCrons.iterator(); scheduledCronsIterator.hasNext();) {
				ScheduledCronJob scheduledCronJob = scheduledCronsIterator.next();
				if (scheduledCronJob.innerCrobJob == cronJobToRemove) {
					scheduledCronsIterator.remove();
					scheduledCronJob.schedule.cancel();
				}
			}
		}
	}
}
