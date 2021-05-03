package biz.aQute.gogo.commands.provider;

import static org.osgi.service.log.Logger.ROOT_LOGGER_NAME;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;

import biz.aQute.gogo.commands.dtoformatter.DTOFormatter;

public class LoggerAdminCommands implements Closeable {

	final BundleContext											context;
	final Map<CommandSession, LogTail>							sessions	= new HashMap<>();
	final ServiceTracker<LoggerAdmin, LoggerAdmin>				logAdminT;
	final ServiceTracker<LogReaderService, LogReaderService>	logReaderServiceT;
	final List<LogEntry>										errors		= new ArrayList<>();

	@Activate
	public LoggerAdminCommands(BundleContext context, DTOFormatter formatter) {
		this.context = context;
		dtos(formatter);
		logAdminT = new ServiceTracker<>(context, LoggerAdmin.class, null);
		logAdminT.open();
		logReaderServiceT = new ServiceTracker<LogReaderService, LogReaderService>(context, LogReaderService.class,
			null) {
			@Override
			public LogReaderService addingService(ServiceReference<LogReaderService> reference) {
				LogReaderService s = super.addingService(reference);
				s.addLogListener(e -> {
					synchronized (errors) {
						if (LogLevel.WARN.implies(e.getLogLevel())) {
							errors.add(e);
							if (errors.size() > 100)
								errors.remove(0);
						}
					}
				});
				return s;

			}
		};
		logReaderServiceT.open();
	}

	void dtos(DTOFormatter f) {
		f.build(LogEntry.class)
			.inspect()
			.method("sequence")
			.format("time", l -> Instant.ofEpochMilli(l.getTime()))
			.method("message")
			.method("bundle")
			.method("servicereference")
			.method("exception")
			.method("logLevel")
			.method("loggerName")
			.method("threadInfo")
			.method("location")
			.part()
			.line()
			.format("time", l -> Instant.ofEpochMilli(l.getTime()))
			.method("message")
			.as(b -> DisplayUtil.dateTime(b.getTime()) + " " + b.getMessage());
	}

	private Optional<LogReaderService> getLogReaderService() throws InterruptedException {
		return Optional.ofNullable(logReaderServiceT.waitForService(1000));
	}

	private Optional<LoggerAdmin> getLoggerAdmin() throws InterruptedException {
		return Optional.ofNullable(logAdminT.waitForService(1000));
	}

	@Override
	public void close() {
		sessions.values()
			.forEach(LogTail::close);
		logAdminT.close();
		logReaderServiceT.close();
	}

	@Descriptor("Show the current log")
	public List<LogEntry> log(
	//@formatter:off
		@Parameter( absentValue="false", presentValue="true",names={"-d","--debug"})
		boolean debug,
		@Parameter( absentValue="false", presentValue="true",names={"-i","--info"})
		boolean info,
		@Parameter( absentValue="false", presentValue="true",names={"-w","--warning"})
		boolean warning
		//@formatter:on

	) throws InterruptedException {
		return getLog().stream()
			.filter(e -> {
				switch (e.getLogLevel()) {
					case DEBUG :
						return debug;

					case INFO :
						return info || debug;
					case TRACE :
						return info || debug;
					case WARN :
						return info || debug || warning;
					default :
						return true;
				}
			})
			.collect(Collectors.toList());

	}

	@Descriptor("Show the last error, warining & audit messages")
	public List<LogEntry> errors(
		// formatter:off
		@Parameter(absentValue = "false", presentValue = "true", names = {
			"-c", "--clear"
		})
		boolean clear

	// formatter:on

	) {
		synchronized (errors) {
			List<LogEntry> errors2 = new ArrayList<>(errors);
			errors.clear();
			return errors2;
		}
	}

	private ArrayList<LogEntry> getLog() throws InterruptedException {
		return getLogReaderService().map(LogReaderService::getLog)
			.map(Collections::list)
			.orElseGet(() -> {
				System.err.println("No log service");
				return new ArrayList<>();
			});
	}

	/**
	 * TAIL
	 */
	@Descriptor("Continuously show the log messages")
	public void tail(
	//@formatter:off

		CommandSession s,

		@Descriptor("Filter the message with a glob")
		@Parameter(absentValue="*", names= {"-f", "--filter"})
		String filter,

		@Descriptor("Number of times the filter must match")
		@Parameter(absentValue="1", names= {"-c", "--count"})
		int count,


		@Descriptor("The minimum log level (DEBUG,INFO,WARN,ERROR)")
		LogLevel level

		//@formatter:on
	) throws IOException, InterruptedException {

		LogTail lt = sessions.remove(s);
		if (lt == null) {
			getLogReaderService().ifPresent(service -> sessions.put(s, new LogTail(s, service, level, filter, count)));
		} else {
			lt.close();
		}
	}

	@Descriptor("Continuously show all the log messages")
	public void tail(
	//@formatter:off
		CommandSession s,
		@Parameter( absentValue="false", presentValue="true",names={"-d","--debug"})
		boolean debug,
		@Parameter( absentValue="false", presentValue="true",names={"-i","--info"})
		boolean info,
		@Parameter( absentValue="false", presentValue="true",names={"-w","--warning"})
		boolean warning
		//@formatter:on
	) throws IOException, InterruptedException {

		LogLevel level = LogLevel.DEBUG;
		if (info)
			level = LogLevel.INFO;
		if (warning)
			level = LogLevel.WARN;
		tail(s, "*", 1, level);
	}

	/**
	 * TAIL
	 */
	@Descriptor("Continuously show the log messages")
	public void tailclear(
	//@formatter:off

		CommandSession s

		//@formatter:on
	) throws IOException, InterruptedException {

		LogTail lt = sessions.remove(s);
		if (lt != null) {
			lt.close();
		}
	}

	@Descriptor("Add a log level for a logger name prefix for a specific bundle")
	public Map<String, LogLevel> addlevel(
	//@formatter:off

		@Descriptor("The logger context bundle")
		Bundle b,

		@Descriptor("The name of the logger prefix or ROOT for all")
		String name,

		@Descriptor("The log level to set (DEBUG,INFO,WARN,ERROR)")
		LogLevel level

		//@formatter:on
	) throws InterruptedException {
		return add(b, name, level);
	}

	@Descriptor("Add a log level of a logger name prefix")
	public Map<String, LogLevel> addlevel(
	//@formatter:off

		@Descriptor("The name of the logger prefix or ROOT for all")
		String name,

		@Descriptor("The log level to set (DEBUG,INFO,WARN,ERROR)")
		LogLevel level

		//@formatter:on
	) throws InterruptedException {
		return add(null, name, level);
	}

	@Descriptor("Remove the log level of a logger name prefix for a specific bundle")
	public Map<String, LogLevel> rmlevel(
	//@formatter:off

		@Descriptor("The logger context bundle")
		Bundle b,

		@Descriptor("The name of the logger prefix or ROOT for all")
		String name
		//@formatter:on
	) throws InterruptedException {
		return rm(b, name);
	}

	@Descriptor("Remove a log level for a logger name prefix")
	public Map<String, LogLevel> rmlevel(
	//@formatter:off
		@Descriptor("The name of the logger prefix or ROOT for all")
		String name
		//@formatter:on
	) throws InterruptedException {
		return rm(null, name);
	}

	@Descriptor("Show the log levels for a given bundle")
	public Map<String, LogLevel> levels(
	//@formatter:off

		@Descriptor("The logger context bundle")
		Bundle b

		//@formatter:on
	) throws InterruptedException {
		return getLoggerAdmin().map(lAdmin -> {
			LoggerContext loggerContext = getLoggerContext(b, lAdmin);
			if (loggerContext.isEmpty())
				loggerContext = lAdmin.getLoggerContext(null);

			return loggerContext.getLogLevels();
		})
			.orElseGet(() -> {
				System.err.println("No log admin");
				return new HashMap<>();
			});
	}

	@Descriptor("Show all log levels")
	public Map<String, Map<String, LogLevel>> levels() throws InterruptedException {

		return getLoggerAdmin().map(lAdmin -> {
			Map<String, Map<String, LogLevel>> map = new LinkedHashMap<>();
			map.put(ROOT_LOGGER_NAME, lAdmin.getLoggerContext(null)
				.getLogLevels());
			for (Bundle b : context.getBundles()) {
				LoggerContext lctx = getLoggerContext(b, lAdmin);
				if (lctx.isEmpty())
					continue;

				Map<String, LogLevel> logLevels = lctx.getLogLevels();
				map.put(b.getSymbolicName(), logLevels);
			}
			return map;
		})
			.orElseGet(() -> {
				System.err.println("No log admin");
				return new HashMap<>();
			});

	}

	@Descriptor("Show the default level")
	public String defaultlevel() throws InterruptedException {

		return getLoggerAdmin().map(lAdmin -> {
			try {
				return lAdmin.getLoggerContext(null)
					.getEffectiveLogLevel("")
					.toString();
			} catch (Exception e0) {
				e0.printStackTrace();
				return e0.toString();
			}
		})
			.orElseGet(() -> {
				System.err.println("No log admin");
				return "";
			});
	}

	@Descriptor("Set the default level")
	public String defaultlevel(@Descriptor("The log level to set (DEBUG,INFO,WARN,ERROR)")
	LogLevel level) throws InterruptedException {

		return getLoggerAdmin().map(lAdmin -> {

			LoggerContext rootContext = lAdmin.getLoggerContext(null);
			Map<String, LogLevel> map = rootContext.getLogLevels();
			map.put(ROOT_LOGGER_NAME, level);
			rootContext.setLogLevels(map);
			try {
				return defaultlevel();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return "";
			}
		})
			.orElseGet(() -> {
				System.err.println("No log admin");
				return "";
			});
	}

	@Descriptor("Get the level for a name")
	public String level(
	//@formatter:off

		@Descriptor("A name for a logger")
		String name

		//@formatter:off
		) throws InterruptedException {
		return getLoggerAdmin().map( lAdmin -> {
			LoggerContext loggerContext = getLoggerContext( null, lAdmin);
			return loggerContext.getEffectiveLogLevel(name).toString();
		}).orElse( null);
	}

	@Descriptor("Get the level for a name")
	public String level(
		//@formatter:off

		@Descriptor("The bundle ")
		Bundle b,

		@Descriptor("A name for a logger")
		String name

		//@formatter:off
		) throws InterruptedException {
		return getLoggerAdmin().map( lAdmin -> {
			LoggerContext loggerContext = getLoggerContext( b, lAdmin);
			return loggerContext.getEffectiveLogLevel(name).toString();
		}).orElse( null);
	}

	private Map<String, LogLevel> rm(Bundle ctx, String name) throws InterruptedException {

		return getLoggerAdmin().map(lAdmin -> {
			LoggerContext loggerContext = getLoggerContext(ctx, lAdmin);
			Map<String, LogLevel> logLevels = loggerContext.getLogLevels();
			logLevels.remove(name);
			loggerContext.setLogLevels(logLevels);
			return loggerContext.getLogLevels();

		})
			.orElseGet(() -> {
				System.err.println("No log admin");
				return new HashMap<>();
			});
	}

	private Map<String, LogLevel> add(Bundle b, String name, LogLevel level) throws InterruptedException {

		return getLoggerAdmin().map(lAdmin -> {

			LoggerContext loggerContext = getLoggerContext(b, lAdmin);
			Map<String, LogLevel> logLevels = loggerContext.getLogLevels();
			logLevels.put(name, level);
			loggerContext.setLogLevels(logLevels);
			return loggerContext.getLogLevels();
		})
			.orElseGet(() -> {
				System.err.println("No log admin");
				return new HashMap<>();
			});
	}

	private LoggerContext getLoggerContext(Bundle b, LoggerAdmin lAdmin) {
		if ( b == null) {
			return lAdmin.getLoggerContext(null);
		}
		StringBuilder sb = new StringBuilder(b.getSymbolicName());
		sb.append("|").append(b.getVersion()).append("|").append(b.getLocation());
		return lAdmin.getLoggerContext(sb.toString());
	}

	@Descriptor("Create an SLF4J debug entry (for testing)")
	public void slf4jdebug(
		//@formatter:off
		@Parameter(absentValue="LoggerAdminCommands", names= {"-n", "--name"})
		@Descriptor("The name of the logger")
		String name,

		@Descriptor("The message to log")
		Object message
		//formatter:on
	) {
		org.slf4j.Logger logger = LoggerFactory.getLogger(name);
		logger.debug("{}", message);
	}

	@Descriptor("Create an SLF4J warn entry")
	public void slf4jwarn(
		//@formatter:off
		@Parameter(absentValue="LoggerAdminCommands", names= {"-n", "--name"})
		@Descriptor("The name of the logger")
		String name,

		@Descriptor("The message to log")
		Object message
		//formatter:on
		) {
		org.slf4j.Logger logger = LoggerFactory.getLogger(name);
		logger.warn("{}", message);
	}

	@Descriptor("Create an SLF4J info entry")
	public void slf4jinfo(
		//@formatter:off
		@Parameter(absentValue="LoggerAdminCommands", names= {"-n", "--name"})
		@Descriptor("The name of the logger")
		String name,

		@Descriptor("The message to log")
		Object message
		//formatter:on

		) {
		org.slf4j.Logger logger = LoggerFactory.getLogger(name);
		logger.info("{}", message);
	}

	@Descriptor("Create an SLF4J error entry")
	public void slf4jerror(
		//@formatter:off
		@Parameter(absentValue="LoggerAdminCommands", names= {"-n", "--name"})
		@Descriptor("The name of the logger")
		String name,

		@Descriptor("The message to log")
		Object message
		//formatter:on


		) {
		org.slf4j.Logger logger = LoggerFactory.getLogger(name);
		logger.error("{}", message);
	}
}
