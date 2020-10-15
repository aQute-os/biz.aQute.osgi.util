package biz.aQute.gogo.commands.provider;

import org.apache.felix.service.command.CommandSession;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

class LogTail implements LogListener {
	final CommandSession	session;
	final LogReaderService	ls;
	final LogLevel			level;

	LogTail(CommandSession session, LogReaderService ls, LogLevel level) {
		this.session = session;
		this.ls = ls;
		this.level = level;
		this.ls.addLogListener(this);
	}

	@Override
	public void logged(LogEntry entry) {
		if (level.implies(entry.getLogLevel())) {
			this.session.getConsole()
				.printf("%6s %40s %s\n", entry.getLogLevel(), entry.getLoggerName(), entry.getMessage());
		}
	}

	public void close() {
		ls.removeLogListener(this);
	}

}
