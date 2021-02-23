package biz.aQute.gogo.commands.provider;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;

import biz.aQute.gogo.commands.dtoformatter.DTOFormatter;

public class Log implements Closeable {
	final ServiceTracker<LogReaderService, LogReaderService>	log;

	final BundleContext context;

	public Log(BundleContext context, DTOFormatter formatter) {
		this.context = context;
		this.log = new ServiceTracker<>(context, LogReaderService.class, null);
		dtos(formatter);
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

	@Override
	public void close() throws IOException {
		log.close();
	}

	@Descriptor("Show the current log")
	public List<LogEntry> log() throws InterruptedException {
		LogReaderService logs = log.waitForService(1000);
		if (logs == null) {
			System.err.println("No log service");
			return Collections.emptyList();
		}
		Enumeration<LogEntry> log2 = logs
			.getLog();
		return Collections.list(log2);
	}

}
