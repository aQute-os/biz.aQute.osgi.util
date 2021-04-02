package biz.aQute.osgi.logger.components.provider;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;

class LogMessageFormatter {
	final static Pattern	DFEXTRACT_P	= Pattern.compile("\\((?<dateformat>[^)]+)\\)(?<rest>.*)");
	final String			format;
	boolean					reported	= false;
	final DateTimeFormatter	dateFormatter;

	LogMessageFormatter(String format) {
		DateTimeFormatter intermediate;
		Matcher m = DFEXTRACT_P.matcher(format);
		if (m.matches()) {
			intermediate = DateTimeFormatter.ofPattern(m.group("dateformat"));
			this.format = m.group("rest");
		} else {
			this.format = format;
			intermediate = DateTimeFormatter.ofPattern("MM/dd HHmmss");
		}
		dateFormatter = intermediate.withLocale(Locale.US)
			.withZone(ZoneId.of("UTC"));
	}

	public String format(LogEntry entry) {
		try {
			return String.format(Locale.US, format, //
				getTime(entry), // 1
				entry.getSequence(), // 2
				getLogLevel(entry), // 3
				getBundle(entry), // 4
				getThreadInfo(entry), // 5
				getLoggerName(entry), // 6
				getMessage(entry), //
				getLocation(entry), //
				getServiceReference(entry), //
				extractStackTrace(entry.getException()));
		} catch (Exception e) {
			if (!reported)
				System.err.println("Formatting log message failed: " + e.getMessage() + " in " + format);
			reported = true;
			return entry.toString();
		}
	}

	private String getMessage(LogEntry entry) {
		String message = entry.getMessage();
		if (message != null)
			return message;
		return "";
	}

	private String getLoggerName(LogEntry entry) {
		String loggerName = entry.getLoggerName();
		if (loggerName != null) {
			return shorten(loggerName);
		}
		return "";
	}

	private String shorten(String loggerName) {
		int n = loggerName.lastIndexOf('.');
		return loggerName.substring(n + 1);
	}

	private String getLocation(LogEntry entry) {
		StackTraceElement location = entry.getLocation();
		if (location == null)
			return "";

		return location.toString();
	}

	private String getServiceReference(LogEntry entry) {
		return entry.getServiceReference() != null ? " (" + shorten(entry.getServiceReference() + ")") : "";
	}

	private String getThreadInfo(LogEntry entry) {
		return entry.getThreadInfo() == null ? "" : entry.getThreadInfo();
	}

	private String getLogLevel(LogEntry entry) {
		LogLevel logLevel = entry.getLogLevel();
		if (logLevel != null)
			return logLevel.name();

		return "UNKNOWN";
	}

	private String getBundle(LogEntry entry) {
		return (entry.getBundle() != null ? Long.toString(entry.getBundle()
			.getBundleId()) : "");
	}

	private String getTime(LogEntry entry) {
		Instant instant = Instant.ofEpochMilli(entry.getTime());
		return dateFormatter.format(instant);
	}

	private static String extractStackTrace(final Throwable throwable) {
		if (throwable == null) {
			return "";
		}

		try (final StringWriter stringWriter = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(stringWriter)) {
			printWriter.println();
			throwable.printStackTrace(printWriter);
			return stringWriter.toString();
		} catch (IOException e) {
			// Should never happen - return at least the exception message
			return "extractStackTrace failed: throwable.message=" + throwable.getMessage();
		}
	}

}
