package biz.aQute.gogo.commands.provider;

import org.apache.felix.service.command.CommandSession;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import aQute.libg.glob.Glob;

class LogTail implements LogListener {
	final CommandSession	session;
	final LogReaderService	ls;
	final LogLevel			level;
	final Glob				glob;
	volatile int			count;

	LogTail(CommandSession session, LogReaderService ls, LogLevel level, String filter, int count) {
		this.session = session;
		this.ls = ls;
		this.level = level;
		this.ls.addLogListener(this);
		this.glob = filter == null || filter.equals("*") ? Glob.ALL : new Glob(filter);
		this.count = count > 1 ? count : 1;
	}

	@Override
	public void logged(LogEntry entry) {
		if (level.implies(entry.getLogLevel())) {
			if (glob == Glob.ALL || glob.matches(entry.getMessage())) {
				if (count == 1) {
					this.session.getConsole()
						.printf("%6s %40s %s\n", entry.getLogLevel(), entry.getLoggerName(), entry.getMessage());
				} else
					count--;
			}
		}
	}

	public void close() {
		ls.removeLogListener(this);
	}

}
