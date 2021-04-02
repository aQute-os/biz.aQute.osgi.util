package biz.aQute.scheduler.basic.provider;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = CentralScheduler.class, scope = ServiceScope.SINGLETON)
class CentralScheduler {
	final static Logger				logger			= LoggerFactory.getLogger(SchedulerImpl.class);
	final ScheduledExecutorService	scheduler	;
	final PromiseFactory			factory;

	long							shutdownTimeout	= 5000;

	@Activate
	public CentralScheduler() {
		scheduler = Executors.newScheduledThreadPool(50);
		factory = new PromiseFactory(scheduler);
	}

	@Deactivate
	void deactivate() {
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

}
