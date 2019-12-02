package biz.aQute.osgi.logger.tracker.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings({"rawtypes","unchecked"})
public class LoggerFactory implements Closeable {
	final BlockingQueue<Consumer<LogService>>	queue		= new LinkedBlockingQueue<>();
	final BundleContext							context;
	final ServiceTracker<?,?>						tracker;
	final Thread								thread;

	private final LogService					lastResort	= new LogService() {

																@Override
																public void log(ServiceReference ref, int level,
																		String msg,
																		Throwable throwable) {
																	System.err.printf("%s:%s %s: %s\n", level(level),
																			ref,
																			throwable, msg);
																}

																@Override
																public void log(ServiceReference ref, int level,
																		String msg) {
																	System.err.printf("%s:%s:%s: %s\n", level(level),
																			ref, msg);
																}

																@Override
																public void log(int level, String msg,
																		Throwable throwable) {
																	System.err.printf("%s:%s:%s: %s\n", level(level),
																			throwable,
																			msg);
																}

																@Override
																public void log(int level, String msg) {
																	System.err.printf("%s:%s:: %s\n", level(level),
																			msg);
																}
															};

	public LoggerFactory(BundleContext context) {
		this.context = context;
		this.tracker = new ServiceTracker(context, LogService.class.getName(), null);
		this.thread = new Thread(this::run, "LoggerFactory");
		this.thread.start();
	}

	@Override
	public void close() throws IOException {
		thread.interrupt();
		try {
			thread.join(10000);
		} catch (InterruptedException e) {
			// ignore
		}
		tracker.close();
	}

	public Logger logger(Class<?> clazz) {
		return new Logger(this, clazz.getSimpleName());
	}

	private void run() {
		while (!thread.isInterrupted()) {
			try {
				Consumer<LogService> take = queue.take();
				LogService l = (LogService) tracker.waitForService(10000);
				if (l == null) {
					l = lastResort;
				}

				do {
					take.accept(l);
				} while ((take = queue.poll()) != null);

			} catch (InterruptedException e) {
				thread.interrupt();
				return;
			} catch (Exception e) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					thread.interrupt();
					return;
				}
			}
		}
	}

	String level(int level) {
		switch (level) {
		default:
			return "??" + level + "?";

		case LogService.LOG_DEBUG:
			return "DEBUG";

		case LogService.LOG_WARNING:
			return "WARN";
		case LogService.LOG_ERROR:
			return "ERROR";
		case LogService.LOG_INFO:
			return "INFO";
		}
	}

}
