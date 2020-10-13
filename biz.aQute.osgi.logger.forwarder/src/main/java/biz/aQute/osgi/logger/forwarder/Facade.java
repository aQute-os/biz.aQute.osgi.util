package biz.aQute.osgi.logger.forwarder;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.osgi.framework.Bundle;
import org.osgi.service.log.Logger;
import org.slf4j.helpers.MarkerIgnoringBase;

/**
 * Facade for forwarding log messages to a delegate set by {@link LogForwarder}.
 */
final class Facade extends MarkerIgnoringBase {
	private static final long		serialVersionUID		= 1;
	private static final int		TRACE_LEVEL_THRESHOLD	= Level.FINEST.intValue();
	private static final int		DEBUG_LEVEL_THRESHOLD	= Level.FINE.intValue();
	private static final int		INFO_LEVEL_THRESHOLD	= Level.INFO.intValue();
	private static final int		WARN_LEVEL_THRESHOLD	= Level.WARNING.intValue();
	private final SimpleFormatter	formatter				= new SimpleFormatter();
	final transient Bundle			bundle;

	/*
	 * All {@link org.slf4j.Logger} calls to this class are forwarded to this
	 * delegate. delegate is the {@link org.osgi.service.log.Logger} to be
	 * switched to either a {@link QueuingLogger} or one that forwards directly
	 * to the OSGi LogService. delegate can be assigned and read from different
	 * threads and places in package-private scope, therefore needs to be
	 * volatile
	 */
	volatile transient Logger		delegate;

	public Facade(Bundle bundle, String loggerName) {
		this.name = loggerName;
		this.bundle = bundle;
	}

	public Bundle getBundle() {
		return this.bundle;
	}

	@Override
	public void debug(String format, Object... arguments) {
		if (checkIfArgumentsNullOrEmpty(arguments)) {
			delegate.debug(format);
		} else {
			delegate.debug(format, arguments);
		}
	}

	@Override
	public void debug(String message) {
		delegate.debug(message);
	}

	@Override
	public void debug(String format, Object arguments) {
		delegate.debug(format, arguments);
	}

	@Override
	public void debug(String message, Throwable t) {
		delegate.debug(message, t);
	}

	@Override
	public void debug(String format, Object a, Object b) {
		delegate.debug(format, a, b);
	}

	@Override
	public void error(String format, Object... arguments) {
		if (checkIfArgumentsNullOrEmpty(arguments)) {
			delegate.error(format);
		} else {
			delegate.error(format, arguments);
		}
	}

	@Override
	public void error(String message) {
		delegate.error(message);
	}

	@Override
	public void error(String format, Object arguments) {
		delegate.error(format, arguments);
	}

	@Override
	public void error(String message, Throwable t) {
		delegate.error(message, t);
	}

	@Override
	public void error(String format, Object a, Object b) {
		delegate.error(format, a, b);
	}

	@Override
	public void info(String format, Object... arguments) {
		if (checkIfArgumentsNullOrEmpty(arguments)) {
			delegate.info(format);
		} else {
			delegate.info(format, arguments);
		}
	}

	@Override
	public void info(String message) {
		delegate.info(message);
	}

	@Override
	public void info(String format, Object arguments) {
		delegate.info(format, arguments);
	}

	@Override
	public void info(String message, Throwable t) {
		delegate.info(message, t);
	}

	@Override
	public void info(String format, Object a, Object b) {
		delegate.info(format, a, b);
	}

	@Override
	public void warn(String format, Object... arguments) {
		if (checkIfArgumentsNullOrEmpty(arguments)) {
			delegate.warn(format);
		} else {
			delegate.warn(format, arguments);
		}
	}

	@Override
	public void warn(String message) {
		delegate.warn(message);
	}

	@Override
	public void warn(String format, Object arguments) {
		delegate.warn(format, arguments);
	}

	@Override
	public void warn(String message, Throwable t) {
		delegate.warn(message, t);
	}

	@Override
	public void warn(String format, Object a, Object b) {
		delegate.warn(format, a, b);
	}

	@Override
	public void trace(String format, Object... arguments) {
		if (checkIfArgumentsNullOrEmpty(arguments)) {
			delegate.trace(format);
		} else {
			delegate.trace(format, arguments);
		}
	}

	@Override
	public void trace(String message) {
		delegate.trace(message);
	}

	@Override
	public void trace(String format, Object arguments) {
		delegate.trace(format, arguments);
	}

	@Override
	public void trace(String message, Throwable t) {
		delegate.trace(message, t);
	}

	@Override
	public void trace(String format, Object a, Object b) {
		delegate.trace(format, a, b);
	}

	@Override
	public boolean isInfoEnabled() {
		return delegate.isInfoEnabled();
	}

	@Override
	public boolean isDebugEnabled() {
		return delegate.isDebugEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return delegate.isErrorEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return delegate.isTraceEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return delegate.isWarnEnabled();
	}

	/*
	 * Checks whether the specified varargs is empty or null. This is primarily
	 * used as a hack to avoid NPE in special cases.
	 * @param arguments the arguments to check
	 * @return {@code true} if {@code null} or empty, otherwise {@code false}
	 * @see "https://issues.apache.org/jira/browse/FELIX-6088"
	 */
	private boolean checkIfArgumentsNullOrEmpty(Object... arguments) {
		return arguments == null || arguments.length == 0;
	}

	void publish(LogRecord record) {
		try {
			int julLevelValue = record.getLevel()
				.intValue();
			if (julLevelValue <= TRACE_LEVEL_THRESHOLD) {
				delegate.trace(formatter.format(record), record.getThrown());
			} else if (julLevelValue <= DEBUG_LEVEL_THRESHOLD) {
				delegate.debug(formatter.format(record), record.getThrown());
			} else if (julLevelValue <= INFO_LEVEL_THRESHOLD) {
				delegate.info(formatter.format(record), record.getThrown());
			} else if (julLevelValue <= WARN_LEVEL_THRESHOLD) {
				delegate.warn(formatter.format(record), record.getThrown());
			} else {
				error(formatter.format(record), record.getThrown());
			}
		} catch (Exception e) {
			e.printStackTrace();
			// ignore exception, also don't log it to prevent
			// cycles
		}
	}
}
