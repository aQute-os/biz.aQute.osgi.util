package biz.aQute.scheduler.basic.provider;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.converter.Converters;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.aQute.scheduler.api.CancelException;
import biz.aQute.scheduler.api.CronAdjuster;
import biz.aQute.scheduler.api.CronJob;
import biz.aQute.scheduler.api.Scheduler;
import biz.aQute.scheduler.api.TimeoutException;
import biz.aQute.scheduler.api.annotation.CronExpression;

/**
 * The Class SimpleJobScheduler.
 */
@Component(
		service = Scheduler.class,
		immediate = true)
public class SchedulerImpl implements Scheduler {

	final List<Cron<?>> crons = new ArrayList<>();

	final Logger logger = LoggerFactory.getLogger(SchedulerImpl.class);

	final Clock clock = Clock.systemDefaultZone();

	/** The executor. */
	final ScheduledExecutorService executor;


	/**
	 * Instantiates a new simple job scheduler.
	 */
	public SchedulerImpl() {
		executor = Executors.newScheduledThreadPool(10);
	}


	/**
	 * Deactivate.
	 */
	@Deactivate
	void deactivate() {
		final List<Runnable> shutdownNow = executor.shutdownNow();

		if (shutdownNow != null && shutdownNow.size() > 0) {
			logger.warn("Shutdown executables: " + shutdownNow);
		}

		logger.debug("DeActivated");
	}


	/**
	 * Adds the schedule.
	 *
	 * @param <T> the generic type
	 * @param s   the s
	 * @param map the map
	 * @throws Exception the exception
	 */
	@Reference(
			policy = ReferencePolicy.DYNAMIC,
			cardinality = ReferenceCardinality.MULTIPLE)
	public <T> void addSchedule(final CronJob<T> s, final Map<String, Object> map) throws Exception {
		final String[] schedules = Converters.standardConverter().convert(map.get(CronExpression.PROPERTY_NAME)).to(String[].class);

		if (schedules == null || schedules.length == 0) {
			return;
		}

		final Class<T> type = getType(s);

		synchronized (crons) {
			for (final String schedule : schedules) {
				try {
					final Cron<T> cron = new Cron<>(type, s, schedule);
					crons.add(cron);
				} catch (final Exception e) {
					logger.error("Invalid  cron expression: " + schedule + " from " + map, e);
				}
			}
		}

	}


	/**
	 * Removes the schedule.
	 *
	 * @param s the s
	 */
	public void removeSchedule(final CronJob<?> s) {
		synchronized (crons) {
			for (final Iterator<Cron<?>> cron = crons.iterator(); cron.hasNext();) {
				try {
					final Cron<?> c = cron.next();
					if (c.target == s) {
						cron.remove();
						c.schedule.close();
					}
				} catch (final IOException e) {
					// We're closing, so ignore any errors.
				}
			}
		}
	}


	/**
	 * After.
	 *
	 * @param ms the ms
	 * @return the cancellable promise impl
	 */
	@Override
	public CancellablePromiseImpl<Instant> after(final long ms) {
		final Deferred<Instant> deferred = new Deferred<>();

		final Instant start = Instant.now();

		final ScheduledFuture<?> schedule = executor.schedule(() -> {
			deferred.resolve(start);
		}, ms, TimeUnit.MILLISECONDS);

		return new CancellablePromiseImpl<Instant>(deferred.getPromise()) {
			@Override
			public boolean cancel() {
				try {
					return schedule.cancel(true);
				} catch (final Exception e) {
					return false;
				}
			}
		};
	}


	/**
	 * After.
	 *
	 * @param <T>      the generic type
	 * @param callable the callable
	 * @param ms       the ms
	 * @return the cancellable promise impl
	 */
	@Override
	public <T> CancellablePromiseImpl<T> after(final Callable<T> callable, final long ms) {
		final Deferred<T> deferred = new Deferred<>();

		final ScheduledFuture<?> schedule = executor.schedule(() -> {
			try {
				deferred.resolve(callable.call());
			} catch (final Throwable e) {
				deferred.fail(e);
			}
		}, ms, TimeUnit.MILLISECONDS);

		return new CancellablePromiseImpl<T>(deferred.getPromise()) {
			@Override
			public boolean cancel() {
				try {
					return schedule.cancel(true);
				} catch (final Exception e) {
					return false;
				}
			}
		};
	}


	/**
	 * Delay.
	 *
	 * @param <T> the generic type
	 * @param ms  the ms
	 * @return the success
	 */
	public <T> Success<T, T> delay(final long ms) {
		return (p) -> {
			final Deferred<T> deferred = new Deferred<T>();
			after(ms).then((pp) -> {
				deferred.resolve(p.getValue());
				return null;
			});
			return deferred.getPromise();
		};
	}


	/**
	 * The Class Unique.
	 */
	public static class Unique {

		/** The done. */
		final AtomicBoolean done = new AtomicBoolean();


		/**
		 * The Interface RunnableException.
		 */
		public interface RunnableException {

			/**
			 * Run.
			 *
			 * @throws Exception the exception
			 */
			public void run() throws Exception;
		}


		/**
		 * Once.
		 *
		 * @param o the o
		 * @return true, if successful
		 * @throws Exception the exception
		 */
		public boolean once(final RunnableException o) throws Exception {
			if (done.getAndSet(true) == false) {
				o.run();
				return true;
			} else {
				return false;
			}
		}
	}


	/**
	 * Return a new Promise that will fail after timeout ms with a {@link TimeoutException}.
	 *
	 * @param <T>     the generic type
	 * @param promise the promise
	 * @param timeout the timeout
	 * @return the cancellable promise impl
	 */
	@Override
	public <T> CancellablePromiseImpl<T> before(final Promise<T> promise, final long timeout) {
		final Deferred<T> d = new Deferred<T>();

		final Unique only = new Unique();

		after(timeout).then((p) -> {
			only.once(() -> d.fail(TimeoutException.SINGLETON));
			return null;
		});

		promise.then((p) -> {
			only.once(() -> d.resolve(p.getValue()));
			return null;
		}, (p) -> {
			only.once(() -> d.fail(p.getFailure()));
		});

		return new CancellablePromiseImpl<T>(d.getPromise()) {
			@Override
			public boolean cancel() {
				try {
					return only.once(() -> d.fail(CancelException.SINGLETON));
				} catch (final Exception e) {
					return false;
				}
			}
		};
	}


	/**
	 * The Class Schedule.
	 */
	public static abstract class Schedule {

		/** The promise. */
		volatile CancellablePromiseImpl<?> promise;

		/** The canceled. */
		volatile boolean canceled;

		/** The start. */
		final long start = System.currentTimeMillis();

		/** The exception. */
		Throwable exception;


		/**
		 * Next.
		 *
		 * @return the long
		 */
		abstract long next();


		/**
		 * Do it.
		 *
		 * @throws Exception the exception
		 */
		abstract void doIt() throws Exception;
	}


	/**
	 * The Class PeriodSchedule.
	 */
	public static class PeriodSchedule extends Schedule {

		/** The last. */
		long last;

		/** The iterator. */
		PrimitiveIterator.OfLong iterator;

		/** The rover. */
		long rover;

		/** The runnable. */
		RunnableWithException runnable;


		/**
		 * Next.
		 *
		 * @return the long
		 */
		@Override
		long next() {
			if (iterator.hasNext()) {
				last = iterator.nextLong();
			}

			return rover += last;
		}


		/**
		 * Do it.
		 *
		 * @throws Exception the exception
		 */
		@Override
		void doIt() throws Exception {
			runnable.run();
		}

	}


	/**
	 * Schedule.
	 *
	 * @param r     the r
	 * @param first the first
	 * @param ms    the ms
	 * @return the closeable
	 */
	@Override
	public Closeable schedule(final RunnableWithException r, final long first, final long... ms) {
		final PeriodSchedule s = new PeriodSchedule();
		s.iterator = Arrays.stream(ms).iterator();
		s.runnable = r;
		s.rover = System.currentTimeMillis() + first;
		s.last = first;

		schedule(s, first + System.currentTimeMillis());

		return () -> {
			s.canceled = true;
			s.promise.cancel();
		};
	}


	/**
	 * Schedule.
	 *
	 * @param s         the s
	 * @param epochTime the epoch time
	 */
	private void schedule(final Schedule s, final long epochTime) {
		s.promise = at(() -> {
			try {
				s.doIt();
			} catch (final Throwable t) {
				if (s.exception != null) {
					logger.warn("Schedule failed: " + s, t);
				}

				s.exception = t;
			}

			schedule(s, s.next());

			return null;
		}, epochTime);
		if (s.canceled) {
			s.promise.cancel();
		}
	}


	/**
	 * The Class ScheduleCron.
	 *
	 * @param <T> the generic type
	 */
	public class ScheduleCron<T> extends Schedule {

		/** The cron. */
		CronAdjuster cron;

		/** The job. */
		CronJob<T> job;

		/** The runnable. */
		RunnableWithException runnable;

		/** The env. */
		T env;


		/**
		 * Next.
		 *
		 * @return the long
		 */
		@Override
		long next() {
			final ZonedDateTime now = ZonedDateTime.now(clock);
			final ZonedDateTime next = now.with(cron);

			return next.toInstant().toEpochMilli();
		}


		/**
		 * Do it.
		 *
		 * @throws Exception the exception
		 */
		@Override
		void doIt() throws Exception {
			if (runnable != null) {
				runnable.run();
			} else {
				job.run(env);
			}
		}

	}


	/**
	 * Schedule.
	 *
	 * @param r              the r
	 * @param cronExpression the cron expression
	 * @return the closeable
	 * @throws Exception the exception
	 */
	@Override
	public Closeable schedule(final RunnableWithException r, final String cronExpression) throws Exception {
		final ScheduleCron<Void> s = new ScheduleCron<>();
		s.cron = new CronAdjuster(cronExpression);
		s.runnable = r;

		schedule(s, s.next());

		return () -> {
			s.canceled = true;
			s.promise.cancel();
		};
	}


	/**
	 * Schedule.
	 *
	 * @param <T>            the generic type
	 * @param type           the type
	 * @param job            the job
	 * @param cronExpression the cron expression
	 * @return the closeable
	 * @throws Exception the exception
	 */
	@Override
	public <T> Closeable schedule(final Class<T> type, final CronJob<T> job, final String cronExpression) throws Exception {
		final ScheduleCron<T> s = new ScheduleCron<>();
		s.cron = new CronAdjuster(cronExpression);
		s.job = job;
		s.env = type != null && type != Object.class ? Converters.standardConverter().convert(s.cron.getEnv()).to(type) : null;

		schedule(s, s.cron.isReboot() ? 1 : s.next());

		return () -> {
			s.canceled = true;
			s.promise.cancel();
		};
	}


	/**
	 * At.
	 *
	 * @param epochTime the epoch time
	 * @return the cancellable promise impl
	 */
	@Override
	public CancellablePromiseImpl<Instant> at(final long epochTime) {
		final long delay = epochTime - System.currentTimeMillis();
		return after(delay);
	}


	/**
	 * At.
	 *
	 * @param <T>       the generic type
	 * @param callable  the callable
	 * @param epochTime the epoch time
	 * @return the cancellable promise impl
	 */
	@Override
	public <T> CancellablePromiseImpl<T> at(final Callable<T> callable, final long epochTime) {
		final long delay = epochTime - System.currentTimeMillis();
		return after(callable, delay);
	}


	/**
	 * The Class Cron.
	 *
	 * @param <T> the generic type
	 */
	public class Cron<T> {

		/** The target. */
		CronJob<T> target;

		/** The schedule. */
		Closeable schedule;


		/**
		 * Instantiates a new cron.
		 *
		 * @param type           the type
		 * @param target         the target
		 * @param cronExpression the cron expression
		 * @throws Exception the exception
		 */
		public Cron(final Class<T> type, final CronJob<T> target, final String cronExpression) throws Exception {
			this.target = target;
			schedule = schedule(type, target, cronExpression);
		}


		/**
		 * Close.
		 *
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		void close() throws IOException {
			schedule.close();
		}

	}


	/**
	 * Gets the type.
	 *
	 * @param <T> the generic type
	 * @param cj  the cj
	 * @return the type
	 */
	@SuppressWarnings("unchecked")
	private <T> Class<T> getType(final CronJob<T> cj) {
		for (final java.lang.reflect.Type c : cj.getClass().getGenericInterfaces()) {
			if (c instanceof ParameterizedType) {
				if (((ParameterizedType) c).getRawType() == CronJob.class) {
					return (Class<T>) ((ParameterizedType) c)
							.getActualTypeArguments()[0];
				}
			}
		}

		return null;
	}

}
