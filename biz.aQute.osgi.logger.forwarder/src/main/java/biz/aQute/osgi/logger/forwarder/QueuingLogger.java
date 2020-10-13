package biz.aQute.osgi.logger.forwarder;

import java.util.Queue;

import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerConsumer;

/**
 * This OSGi {@link Logger} implementation puts the incoming logger calls into a
 * static queue. This static queue is later consumed by a different real
 * OSGi-enabled bundle when the full OSGi framework (esp. LogService) becomes
 * available.
 * <p>
 * <b>Caution</b>: At the time of creating the runnable in this class, the
 * {@link Facade#delegate} is set to the {@link QueuingLogger}. Before executing
 * the queued runnable, the delegate in the runnable statement is to be switched
 * to a Logger directly logging to the OSGi LogService. This is asserted in
 * {@link LogForwarder#invariant()}.
 * <p>
 * This implies also that the code should <b>never</b> be changed to something
 * like:
 *
 * <pre>
 * <code>
 * public void debug(String message) {
 *     putIntoQueue(() -> {
 *         // at the time of assignment set to QueuingLogger and now will not be switched
 *         final Logger delegateLogger = loggerFacade.delegate;
 *         delegateLogger.debug(message));
 *     });
 * }
 * </code>
 * </pre>
 *
 * which would result in an endless loop and an overflowing log queue.
 */
final class QueuingLogger implements Logger {

	private final Queue<Runnable>	queue;
	private final Facade			loggerFacade;

	public QueuingLogger(Queue<Runnable> queue, final Facade loggerFacade) {
		this.queue = queue;
		this.loggerFacade = loggerFacade;
	}

	@Override
	public void debug(String format, Object... arguments) {
		putIntoQueue(() -> loggerFacade.delegate.debug(format, arguments));
	}

	@Override
	public void debug(String message) {
		putIntoQueue(() -> loggerFacade.delegate.debug(message));
	}

	@Override
	public void debug(String format, Object arguments) {
		putIntoQueue(() -> loggerFacade.delegate.debug(format, arguments));
	}

	@Override
	public void debug(String format, Object a, Object b) {
		putIntoQueue(() -> loggerFacade.delegate.debug(format, a, b));
	}

	@Override
	public void error(String format, Object... arguments) {
		putIntoQueue(() -> loggerFacade.delegate.error(format, arguments));
	}

	@Override
	public void error(String message) {
		putIntoQueue(() -> loggerFacade.delegate.error(message));
	}

	@Override
	public void error(String format, Object arguments) {
		putIntoQueue(() -> loggerFacade.delegate.error(format, arguments));
	}

	@Override
	public void error(String format, Object a, Object b) {
		putIntoQueue(() -> loggerFacade.delegate.error(format, a, b));
	}

	@Override
	public void info(String format, Object... arguments) {
		putIntoQueue(() -> loggerFacade.delegate.info(format, arguments));
	}

	@Override
	public void info(String message) {
		putIntoQueue(() -> loggerFacade.delegate.info(message));
	}

	@Override
	public void info(String format, Object arguments) {
		putIntoQueue(() -> loggerFacade.delegate.info(format, arguments));
	}

	@Override
	public void info(String format, Object a, Object b) {
		putIntoQueue(() -> loggerFacade.delegate.info(format, a, b));
	}

	@Override
	public void warn(String format, Object... arguments) {
		putIntoQueue(() -> loggerFacade.delegate.warn(format, arguments));
	}

	@Override
	public void warn(String message) {
		putIntoQueue(() -> loggerFacade.delegate.warn(message));
	}

	@Override
	public void warn(String format, Object arguments) {
		putIntoQueue(() -> loggerFacade.delegate.warn(format, arguments));
	}

	@Override
	public void warn(String format, Object a, Object b) {
		putIntoQueue(() -> loggerFacade.delegate.warn(format, a, b));
	}

	@Override
	public void trace(String format, Object... arguments) {
		putIntoQueue(() -> loggerFacade.delegate.trace(format, arguments));
	}

	@Override
	public void trace(String message) {
		putIntoQueue(() -> loggerFacade.delegate.trace(message));
	}

	@Override
	public void trace(String format, Object arguments) {
		putIntoQueue(() -> loggerFacade.delegate.trace(format, arguments));
	}

	@Override
	public void trace(String format, Object a, Object b) {
		putIntoQueue(() -> loggerFacade.delegate.trace(format, a, b));
	}

	@Override
	public void audit(String message) {
		putIntoQueue(() -> loggerFacade.delegate.audit(message));
	}

	@Override
	public void audit(String format, Object arg) {
		putIntoQueue(() -> loggerFacade.delegate.audit(format, arg));
	}

	@Override
	public void audit(String format, Object arg1, Object arg2) {
		putIntoQueue(() -> loggerFacade.delegate.audit(format, arg1, arg2));
	}

	@Override
	public void audit(String format, Object... arguments) {
		putIntoQueue(() -> loggerFacade.delegate.audit(format, arguments));
	}

	boolean reported = false;

	private void putIntoQueue(final Runnable runnable) {
		if (!queue.offer(runnable)) {
			if (reported)
				return;
			System.err.println(QueuingLogger.class.getName() + ": Overflowing log queue!"); // NOSONAR
			reported = true;
		}
	}

	/**
	 * In the following block, do a catch-all to take everything in the queue
	 * before the real configuration is loaded
	 */

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public boolean isTraceEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public String getName() {
		return "CannotBeCalled";
	}

	@Override
	public <E extends Exception> void trace(LoggerConsumer<E> consumer) throws E {
		consumer.accept(this);
	}

	@Override
	public <E extends Exception> void debug(LoggerConsumer<E> consumer) throws E {
		consumer.accept(this);
	}

	@Override
	public <E extends Exception> void info(LoggerConsumer<E> consumer) throws E {
		consumer.accept(this);
	}

	@Override
	public <E extends Exception> void warn(LoggerConsumer<E> consumer) throws E {
		consumer.accept(this);
	}

	@Override
	public <E extends Exception> void error(LoggerConsumer<E> consumer) throws E {
		consumer.accept(this);
	}

}
