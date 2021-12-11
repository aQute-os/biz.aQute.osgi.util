package org.slf4j.impl;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.spi.LoggerFactoryBinder;

import aQute.libg.glob.Glob;
import aQute.service.reporter.Reporter;

public class StaticLoggerBinder implements LoggerFactoryBinder, ILoggerFactory {

	public static String					REQUESTED_API_VERSION	= "1.7.30";					// !final
	public final Map<Glob, Level>			levels					= new LinkedHashMap<>();
	public Reporter							reporter;

	/**
	 * The unique instance of this class.
	 */
	private static final StaticLoggerBinder	SINGLETON				= new StaticLoggerBinder();

	/**
	 * Return the singleton of this class.
	 *
	 * @return the StaticLoggerBinder singleton
	 */
	public static final StaticLoggerBinder getSingleton() {
		return SINGLETON;
	}

	@Override
	public ILoggerFactory getLoggerFactory() {
		return this;
	}

	@Override
	public String getLoggerFactoryClassStr() {
		return StaticLoggerBinder.class.getName();
	}

	@Override
	public Logger getLogger(String xname) {
		return new MarkerIgnoringBase() {
			private static final long serialVersionUID = 1L;

			{
				this.name = xname;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public boolean isTraceEnabled() {
				return isEnabled(Level.TRACE);
			}

			@Override
			public void trace(String msg) {
				log(Level.TRACE, "{}", msg);
			}

			@Override
			public void trace(String format, Object arg) {
				log(Level.TRACE, format, arg);
			}

			@Override
			public void trace(String format, Object arg1, Object arg2) {
				log(Level.TRACE, format, arg1, arg2);
			}

			@Override
			public void trace(String format, Object... arguments) {
				log(Level.TRACE, format, arguments);
			}

			@Override
			public void trace(String msg, Throwable t) {
				log(Level.TRACE, "{} {}", msg, t);
			}

			@Override
			public boolean isDebugEnabled() {
				return isEnabled(Level.DEBUG);
			}

			@Override
			public void debug(String msg) {
				log(Level.DEBUG, "{}", msg);
			}

			@Override
			public void debug(String format, Object arg) {
				log(Level.DEBUG, format, arg);
			}

			@Override
			public void debug(String format, Object arg1, Object arg2) {
				log(Level.DEBUG, format, arg1, arg2);
			}

			@Override
			public void debug(String format, Object... arguments) {
				log(Level.DEBUG, format, arguments);
			}

			@Override
			public void debug(String msg, Throwable t) {
				log(Level.DEBUG, "{} {}", msg, t);
			}

			@Override
			public boolean isInfoEnabled() {
				return isEnabled(Level.INFO);
			}

			@Override
			public void info(String msg) {
				log(Level.INFO, "{}", msg);
			}

			@Override
			public void info(String format, Object arg) {
				log(Level.INFO, format, arg);
			}

			@Override
			public void info(String format, Object arg1, Object arg2) {
				log(Level.INFO, format, arg1, arg2);
			}

			@Override
			public void info(String format, Object... arguments) {
				log(Level.INFO, format, arguments);
			}

			@Override
			public void info(String msg, Throwable t) {
				log(Level.INFO, "{} {}", msg, t);
			}

			@Override
			public boolean isWarnEnabled() {
				return isEnabled(Level.WARN);
			}

			@Override
			public void warn(String msg) {
				log(Level.WARN, "{}", msg);
			}

			@Override
			public void warn(String format, Object arg) {
				log(Level.WARN, format, arg);
			}

			@Override
			public void warn(String format, Object arg1, Object arg2) {
				log(Level.WARN, format, arg1, arg2);
			}

			@Override
			public void warn(String format, Object... arguments) {
				log(Level.WARN, format, arguments);
			}

			@Override
			public void warn(String msg, Throwable t) {
				log(Level.WARN, "{} {}", msg, t);
			}

			@Override
			public boolean isErrorEnabled() {
				return isEnabled(Level.ERROR);
			}

			@Override
			public void error(String msg) {
				log(Level.ERROR, "{}", msg);
			}

			@Override
			public void error(String format, Object arg) {
				log(Level.ERROR, format, arg);
			}

			@Override
			public void error(String format, Object arg1, Object arg2) {
				log(Level.ERROR, format, arg1, arg2);
			}

			@Override
			public void error(String format, Object... arguments) {
				log(Level.ERROR, format, arguments);
			}

			@Override
			public void error(String msg, Throwable t) {
				log(Level.ERROR, "{} {}", msg, t);
			}

			boolean isEnabled(Level level) {
				return levels.entrySet()
					.stream()
					.filter(e -> e.getValue()
						.implies(level)
						&& e.getKey()
							.matches(name))
					.findAny()
					.isPresent();
			}

			void log(Level level, String format, Object... arguments) {
				if (!isEnabled(level))
					return;

				for (int i = 0; i < arguments.length; i++) {
					arguments[i] = fixup(arguments[i]);
				}

				switch (level) {
					case TRACE :
					case DEBUG :
					case INFO : {
						reporter.trace(toFormat(format), arguments);
					}
						break;

					case ERROR :
						reporter.error(toFormat(format), arguments);
						break;
					case WARN :
						reporter.warning(toFormat(format), arguments);
						break;
				}
			}

			private String toFormat(String format) {
				return format.replaceAll("\\{\\}", "%s");
			}

		};
	}

	private static Object fixup(Object in) {
		if (in == null)
			return in;

		if (!in.getClass()
			.isArray())
			return in;

		HashSet<Object> visited = new HashSet<>();
		return print(in, visited);
	}

	static Object print(Object object, Set<Object> visited) {
		if (object == null)
			return null;

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

	public void add(String string, Level level) {
		levels.put(new Glob("*"), level);
	}

}
