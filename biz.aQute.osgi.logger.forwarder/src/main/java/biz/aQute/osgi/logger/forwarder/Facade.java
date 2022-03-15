package biz.aQute.osgi.logger.forwarder;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
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
		if (fixupArguments(arguments)) {
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
	public void debug(String format, Object argument) {
		delegate.debug(format, fixup(argument));
	}

	@Override
	public void debug(String message, Throwable t) {
		delegate.debug(message, t);
	}

	@Override
	public void debug(String format, Object a, Object b) {
		delegate.debug(format, fixup(a), fixup(b));
	}

	@Override
	public void error(String format, Object... arguments) {
		if (fixupArguments(arguments)) {
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
	public void error(String format, Object argument) {
		delegate.error(format, fixup(argument));
	}

	@Override
	public void error(String message, Throwable t) {
		delegate.error(message, t);
	}

	@Override
	public void error(String format, Object a, Object b) {
		delegate.error(format, fixup(a), fixup(b));
	}

	@Override
	public void info(String format, Object... arguments) {
		if (fixupArguments(arguments)) {
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
	public void info(String format, Object argument) {
		delegate.info(format, fixup(argument));
	}

	@Override
	public void info(String message, Throwable t) {
		delegate.info(message, t);
	}

	@Override
	public void info(String format, Object a, Object b) {
		delegate.info(format, fixup(a), fixup(b));
	}

	@Override
	public void warn(String format, Object... arguments) {
		if (fixupArguments(arguments)) {
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
	public void warn(String format, Object argument) {
		delegate.warn(format, fixup(argument));
	}

	@Override
	public void warn(String message, Throwable t) {
		delegate.warn(message, t);
	}

	@Override
	public void warn(String format, Object a, Object b) {
		delegate.warn(format, fixup(a), fixup(b));
	}

	@Override
	public void trace(String format, Object... arguments) {
		if (fixupArguments(arguments)) {
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
	public void trace(String format, Object argument) {
		delegate.trace(format, fixup(argument));
	}

	@Override
	public void trace(String message, Throwable t) {
		delegate.trace(message, t);
	}

	@Override
	public void trace(String format, Object a, Object b) {
		delegate.trace(format, fixup(a), fixup(b));
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

	private boolean fixupArguments(Object... arguments) {
		try {
			if (arguments == null || arguments.length == 0)
				return true;

			HashSet<Object> visited = new HashSet<>();
			for (int i = 0; i < arguments.length; i++) {
				visited.clear();
				arguments[i] = print(arguments[i], visited);
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private Object fixup(Object in) {
		if (in == null)
			return in;

		if (!in.getClass()
			.isArray())
			return in;

		return print(in, null);
	}

	static Object print(Object object, Set<Object> visited) {
		if (object == null)
			return null;

		if (visited == null)
			visited = Collections.newSetFromMap(new IdentityHashMap<>());

		if (!visited.add(object))
			return "cycle : " + object;

		if (object.getClass()
			.isArray()) {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			String del = "";
			for (int i = 0; i < Array.getLength(object); i++) {
				if (sb.length() > 1000) {
					sb.append(" ...");
					return sb.toString();
				}
				sb.append(del)
					.append(print(Array.get(object, i), visited));
				del = ",";
				if (sb.length() > 1000) {
					sb.append(" ...");
					return sb.toString();
				}
			}
			sb.append("]");
			return sb;
		}
		return object;
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
