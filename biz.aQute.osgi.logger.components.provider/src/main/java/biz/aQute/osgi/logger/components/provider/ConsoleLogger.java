package biz.aQute.osgi.logger.components.provider;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.metatype.annotations.Designate;

import aQute.lib.io.IO;
import biz.aQute.osgi.logger.components.config.ConsoleLoggerConfiguration;

/**
 * Provides a console logger. It adds itself as a listener to the LogReader
 *
 */
@Designate(ocd = ConsoleLoggerConfiguration.class, factory = false)
@Component(configurationPid = ConsoleLoggerConfiguration.PID, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ConsoleLogger {
	final LogLevel				level;
	final LogMessageFormatter	formatter;
	PrintStream			out;
	final boolean				close;

	@Activate
	public ConsoleLogger(@Reference LogReaderService lrs, ConsoleLoggerConfiguration config) throws IOException {
		level = config.level();
		formatter = new LogMessageFormatter(config.format());
		PrintStream out;
		boolean close;
		switch (config.to()) {
		case ConsoleLoggerConfiguration.STDOUT:
			out = System.out;
			close = false;
			break;
		case ConsoleLoggerConfiguration.STDERR:
			out = System.err;
			close = false;
			break;

		default:
			File file = IO.getFile(config.to());
			IO.mkdirs(file.getParentFile());
			out = new PrintStream(file);
			close = true;
			break;
		}
		this.out = out;
		this.close = close;
		lrs.addLogListener(this::log);
		Collections.list(lrs.getLog())
				.forEach(this::log);
	}

	void log(LogEntry entry) {
		if (level.implies(entry.getLogLevel()))
			out.println(formatter.format(entry));
	}
}
