package biz.aQute.hlogger.util;

import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Formattable;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to a logger with convenient methods for prepending
 * information about the class with every log message.
 * <h1>Usage:</h1>
 * <p>
 *
 * <pre>
 * final static HLogger	ROOT = HLogger.root(YourClass.class);
 * final HLogger logger;
 *
 * ...
 * this.logger = ROOT.child((f) -> {
 * 	// any formatting you want prepended to every logging message in this class
 * });
 * </pre>
 * </p>
 * After this initialization, calling a logger method like
 * {@link HLogger#info(String, Object...)} will call the formatting you
 * specified in the {@link HLogger#child(Consumer)} call.
 * </p>
 * <p>
 * An extra cool thing here is that you can continue to nest prepended
 * formatting messages. For example:
 *
 * <pre>
 * final static HLogger	ROOT = HLogger.root(YourClass.class);
 * HLogger child1;
 * HLogger child2;
 * ...
 * child1 = ROOT.child((f) -> {
 * 	f.format("child one: ");
 * });
 *
 * child2 = child1.child((f) -> {
 * 	f.format("child two: ");
 * }):
 *
 * ...
 * child1.format("hello"); 	// outputs "child one: hello"
 * child2.format("hello"); 	// outputs "child one: child two: hello"
 * </pre>
 * </p>
 */
public interface HLogger {

	class LoggingException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public Object[]				args;

		LoggingException(String msg, Throwable cause, Object... args) {
			super(msg, cause);
			this.args = args;
		}
		LoggingException(String msg, Object[] args) {
			super(msg);
			this.args = args;
		}

	}

	/**
	 * Takes a {@link Consumer} of a {@link Formatter} and appends it to this
	 * logger's formatters to make a new HLogger. Use this to achieve the
	 * nesting described here: {@link HLogger}
	 *
	 * @param id formatter consumer to create the child with
	 * @return child HLogger with the given formatter
	 */
	HLogger child(Consumer<Formatter> id);

	/**
	 * Returns the locale of this logger.
	 *
	 * @return locale of this logger
	 */
	Locale locale();

	default HLogger child(Supplier<String> id) {
		return child(f -> f.format("%s", id.get()));
	}

	default HLogger child(Object object) {
		return child(f -> f.format("%s", object));
	}

	/**
	 * Gets the internal {@link Logger} used to actually print log messages. If
	 * you want the functionality of this logging class, use
	 * <ul>
	 * <li>{@link HLogger#info(String, Object...)}</li>
	 * <li>{@link HLogger#debug(String, Object...)}</li>
	 * <li>{@link HLogger#warn(String, Object...)}</li>
	 * <li>{@link HLogger#error(String, Object...)}</li>
	 * </ul>
	 *
	 * @return internal logger object
	 */
	Logger logger();

	/**
	 * Gets the formatter consumer related to this HLogger.
	 *
	 * @return {@link Consumer}<{@link Formatter}>
	 */
	Consumer<Formatter> id();

	/**
	 * Creates a root HLogger using a name. Uses
	 * {@link LoggerFactory#getLogger(String)} internally.
	 *
	 * @param name name of logger
	 * @return HLogger with no prepending formatters set
	 */
	static HLogger root(String name) {
		return new LoggerBuilder(LoggerFactory.getLogger(name)).root();
	}

	/**
	 * Creates a root HLogger using a class name. Uses
	 * {@link LoggerFactory#getLogger(Class)} internally.
	 *
	 * @param name name of logger
	 * @return HLogger with no prepending formatters set
	 */
	static HLogger root(Class<?> name) {
		return new LoggerBuilder(LoggerFactory.getLogger(name)).root();
	}

	/**
	 * Creates a root HLogger using an object's class name. Uses
	 * {@link LoggerFactory#getLogger(Class)} internally.
	 *
	 * @param name name of logger
	 * @return HLogger with no prepending formatters set
	 */
	static HLogger root(Object name) {
		return root(name.getClass());
	}

	/**
	 * {@link HLogger}
	 */
	class ChildLogger implements HLogger {
		final Logger				logger;
		final Locale				locale;
		final Consumer<Formatter>	id;
		final boolean				debug;
		final boolean				info;

		ChildLogger(Logger logger, Locale locale, Consumer<Formatter> id) {
			this.logger = logger;
			this.locale = locale;
			this.debug = logger.isDebugEnabled();
			this.info = logger.isInfoEnabled();
			this.id = id;
		}

		@Override
		public HLogger child(Consumer<Formatter> id) {
			return new ChildLogger(logger, locale, combine(this.id, id));
		}

		private Consumer<Formatter> combine(Consumer<Formatter> a, Consumer<Formatter> b) {
			if (a == null)
				return b;

			return (f) -> {
				a.accept(f);
				f.format(":");
				b.accept(f);
			};
		}

		@Override
		public Logger logger() {
			return logger;
		}

		@Override
		public Consumer<Formatter> id() {
			return id == null ? f -> f.format("%s", this) : id;
		}

		@Override
		public Locale locale() {
			return locale;
		}

		@Override
		public String toString() {
			return "";
		}

	}

	/**
	 * Builder class for {@link HLogger}.
	 */
	class LoggerBuilder {
		Logger	logger;
		Locale	locale;

		LoggerBuilder(Logger logger) {
			this.logger = logger;
		}

		public LoggerBuilder locale(Locale locale) {
			this.locale = locale;
			return this;
		}

		public HLogger root() {
			return new ChildLogger(logger, locale == null ? Locale.US : locale, null);
		}
	}

	/**
	 * Behaves like {@link Formatter#format(String, Object...)} but adds the
	 * following functionality:
	 * <ul>
	 * <li>Ending your format string with ellipses ie "...", will append all
	 * unused arguments with a comma delimiter</li>
	 * <li>Any object types supported in
	 * {@link HLogger#format(Object, Formatter, int, int, int)} will be printed
	 * according to that specification instead of {@link Formatter}'s defaults
	 * </ul>
	 *
	 * @param format format string
	 * @param args arguments to insert into the format string
	 * @return formatted string
	 */
	default String format(String format, Object... args) {

		try (Formatter f = new Formatter(locale())) {
			id().accept(f);
			f.format(" ");
			if (args == null || args.length == 0) {
				f.format(format);
			} else {
				List<Object> notused = new ArrayList<>();
				Formattable[] fs;
				if (format.endsWith("...")) {
					StringBuilder sb = new StringBuilder(format);
					sb.delete(format.length() - 3, format.length());
					format = sb.append("%")
						.append(args.length)
						.append("$s")
						.toString();
					fs = new Formattable[args.length + 1];
					fs[args.length] = (formatter, flags, width, precision) -> {
						String del = "";
						for (Object o : notused) {
							if (o != null) {
								formatter.format("%s%s", del, 0);
								del = ",";
							}
						}
					};
				} else {
					fs = new Formattable[args.length];
				}

				for (int i = 0; i < args.length; i++) {
					int n = i;
					notused.add(args[n]);
					fs[i] = (formatter, flags, width, precision) -> {
						Object o = args[n];
						notused.set(n, null);
						format(o, formatter, flags, width, precision);
					};
				}
				f.format(format, (Object[]) fs);
			}
			return f.toString();
		} catch (Exception e) {
			logger().error("coding error {}", e, e);
			return format + " " + e.toString();
		}
	}

	/**
	 * Prints a representation of the given Object using the given Formatter.
	 *
	 * @param o object to represent
	 * @param formatter formatter to print to
	 * @param flags as seen here:
	 *            {@link Formattable#formatTo(Formatter, int, int, int)}
	 * @param width min length as seen here:
	 *            {@link Formattable#formatTo(Formatter, int, int, int)}
	 * @param precision max length as seen here:
	 *            {@link Formattable#formatTo(Formatter, int, int, int)}
	 */
	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	default void format(Object o, Formatter formatter, int flags, int width, int precision) {
		if (o == null) {
			formatter.format("null");
			return;
		}
		if (o instanceof Iterable) {
			formatter.format("[");
			String del = "";
			for (Object oo : ((Iterable) o)) {
				format(oo, formatter, 0, 0, 0);
				formatter.format(del);
				del = ",";
			}
			formatter.format("]");
			return;
		}
		if (o.getClass()
			.isArray()) {
			int l = Array.getLength(o);
			String del = "";
			formatter.format("[");
			for (int i = 0; i < l; i++) {
				Object oo = Array.get(o, i);
				format(oo, formatter, 0, 0, 0);
				formatter.format(del);
				del = ",";
			}
			formatter.format("]");
			return;
		}
		if (o instanceof Map) {
			formatter.format("{");
			String del = "";
			Map<Object, Object> map = (Map) o;
			for (Map.Entry<Object, Object> oo : map.entrySet()) {
				format(oo.getKey(), formatter, 0, 0, 0);
				formatter.format("=");
				format(oo.getValue(), formatter, 0, 0, 0);
				formatter.format(del);
				del = ",";
			}
			formatter.format("}");
			return;
		}
		if (o instanceof Socket) {
			Socket s = (Socket) o;
			format(s.getLocalSocketAddress(), formatter);
			formatter.format(">");
			format(s.getRemoteSocketAddress(), formatter);
			return;
		}
		if (o instanceof ServerSocket) {
			ServerSocket s = (ServerSocket) o;
			format(s.getLocalSocketAddress(), formatter);
			formatter.format("<?");
			return;
		}
		if (o instanceof InetSocketAddress) {
			InetSocketAddress isa = (InetSocketAddress) o;
			format(isa.getAddress(), formatter);
			formatter.format(":%s", isa.getPort());
			return;
		}
		if (o instanceof InetAddress) {
			InetAddress address = (InetAddress) o;
			String hostAddress = address.getHostAddress(); // no DNS lookup
			formatter.format("%s", hostAddress);
			return;
		}
		formatter.format("%s", o);
	}

	/**
	 * Calls {@link #format(Object, Formatter, int, int, int)} with 0s for
	 * flags, width, and precision.
	 *
	 * @param o object to format
	 * @param f formatter to use
	 */
	default void format(Object o, Formatter f) {
		format(o, f, 0, 0, 0);
	}
	default HLogger debug(String format, Object... args) {
		Logger l = logger();
		if (l.isDebugEnabled()) {
			l.debug(format(format, args));
		}
		return this;
	}

	default HLogger info(String format, Object... args) {
		Logger l = logger();
		if (l.isInfoEnabled()) {
			l.info(format(format, args));
		}
		return this;
	}

	default HLogger warn(String format, Object... args) {
		Logger l = logger();
		if (l.isWarnEnabled()) {
			l.warn(format(format, args));
		}
		return this;
	}

	default HLogger error(String format, Object... args) {
		Logger l = logger();
		if (l.isErrorEnabled()) {
			l.error(format(format, args));
		}
		return this;
	}

	default LoggingException fail(String format, Object... args) {
		Logger l = logger();
		String msg = format(format, args);
		l.error(msg);
		Optional<Throwable> exc = Stream.of(args)
			.filter(Throwable.class::isInstance)
			.map(Throwable.class::cast)
			.filter(Objects::nonNull)
			.findAny();
		if (exc.isPresent())
			return new LoggingException(msg, exc.get(), args);
		else
			return new LoggingException(msg, args);
	}

	default HLogger trace(String format, Object... args) {
		Logger l = logger();
		if (l.isTraceEnabled()) {
			l.trace(format(format, args));
		}
		return this;
	}

	/**
	 * Returns string representation of the given object according to the
	 * implementation in {@link #format(Object, Formatter)}
	 *
	 * @param o object to translate to a string
	 * @return string translation
	 */
	default String format(Object o) {
		try (Formatter f = new Formatter()) {
			format(o, f);
			return f.toString();
		}
	}

}
