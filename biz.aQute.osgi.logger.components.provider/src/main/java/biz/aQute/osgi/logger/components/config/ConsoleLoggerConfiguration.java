package biz.aQute.osgi.logger.components.config;

import org.osgi.service.log.LogLevel;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface ConsoleLoggerConfiguration {
	String	PID		= "biz.aQute.osgi.logger.console";

	String	STDOUT	= ":stdout:";
	String	STDERR	= ":stderr:";

	@AttributeDefinition
	LogLevel level() default LogLevel.WARN;

	/**
	 * Format of log message in the log files. This uses the standard Java
	 * String.format support. The order of the parameters is as follows.
	 *
	 * <pre>
	 *	%1$ time					date time format
	 *  %2$ sequence				long
	 *  %3$ level					LogLevel enum
	 *  %4$ bundle id				string (can be empty)
	 *  %5$ thread info				string with current thread info
	 *  %6$ logger name				string, can be empty
	 *  %7$ message					string
	 *  %8$ location				string, can be empty
	 *  %9$ service reference		string, can be empty
	 *  %10$ stack trace				string, multiline, can be empty
	 * </pre>
	 *
	 * The format can be prefixed with a data format, for this, wrap it in
	 * parentheses, like "(MM/dd HH:mm:ss)". See the default format Samples:
	 *
	 * <pre>
	 * 2019-06-10T19:16:37:0304 DEBUG 12 [main,com.heilaiq.logfiles.appender.RollingLogFilesAdminImpl] some message [sref=[javax.servlet.Servlet]]
	 * 2019-06-12T23:17:37:0305 INFO   3 [FelixDispatchQueue,Events.Bundle] BundleEvent STARTED [bundle=com.heilaiq.logfiles.appender]
	 * </pre>
	 *
	 * A few relevant tricks:
	 * <ul>
	 * <li>You can try this out in the jshell
	 * <li>the &lt; does not move the pointer to the next argument, this is used
	 * in the default formatting of the time
	 * <li>Explicit argument indices may be used to re-order output. E.g. "%4$2s
	 * %3$2s %2$2s %1$2s", "a", "b", "c", "d") formats like " d c b a".
	 * </ul>
	 *
	 * @return format as described
	 */
	@AttributeDefinition(name = "Format of log messages", description = "Format of log entries to write", required = false)
	String format() default "(MM/dd HH:mm:ss)%1$s %3$-6s [%6$s] %7$s%9$s%10$s";

	/**
	 * Log to the following file. {@value STDOUT} and {@value STDERR} will
	 * output to stdout or stdin respectively. Otherwise this will be opened as
	 * a file relative to the current working directory.
	 *
	 */
	@AttributeDefinition
	String to() default STDOUT;
}
