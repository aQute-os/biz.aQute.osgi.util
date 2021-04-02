package biz.aQute.osgi.logger.components.provider;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.felix.service.command.Parameter;
import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.metatype.annotations.Designate;

import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import biz.aQute.osgi.logger.components.config.RollingLogReaderConfiguration;

/**
 * Implements a {@link LogReaderService} service that persists {@link LogEntry}s
 * to a disk file and performs a file roll over strategy based upon a maximum
 * file size (in MB) policy. The maximum number of persisted log files can also
 * be specified. When the file size threshold has been reached for the current
 * log file a new log file will be created to replace it. After file roll over,
 * the previous log file will be closed and archived according to the maximum
 * number of log files policy parameter. Default behavior is to perform a file
 * roll over each time the service is activated. This behavior can be changed by
 * setting {@code -runkeep=true} in bnd.bnd.
 */
@Designate(ocd = RollingLogReaderConfiguration.class, factory = false)
@GogoCommand(scope = "rolling", function = {
	"logfiles", "find"
})
@Component(service = RollingLogReaderImpl.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = RollingLogReaderConfiguration.PID)
public class RollingLogReaderImpl extends Thread {
	static final String				CURRENT_TXT				= "current.txt";
	static final int				EXCEPTION_PAUSE_MILLIS	= 1000;
	static final int				MAX_QUEUE_LENGTH		= 10_000;
	static final int				MAX_WAIT_MILLIS			= 2000;
	static final int				MAX_RETAINED_LOGFILES	= 10;
	static final int				MAX_FILESIZE_MEGABYTES	= 1;
	static final int				MAX_STALE_TIME_MILLIS	= 500;
	static final int				MAX_ENTRY_SIZE			= 10000;
	final static DateTimeFormatter	FILENAME_FORMATTER		= DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
		.withLocale(Locale.US)
		.withZone(ZoneId.of("UTC"));
	private static final String		FILENAME_MATCHER		= "\\d{8}T\\d{6}.txt";

	final BlockingQueue<LogEntry>	queue					= new LinkedBlockingQueue<>(MAX_QUEUE_LENGTH);
	final LogReaderService			lrs;
	final BundleContext				context;
	final File						root;
	final LogMessageFormatter		formatter;

	int								limit					= MAX_FILESIZE_MEGABYTES * 1_000_000;
	Clock							clock					= Clock.systemUTC();
	volatile int					nextEntry;
	volatile int					nextFile;

	@Activate
	public RollingLogReaderImpl(BundleContext context, @Reference LogReaderService lrs, RollingLogReaderConfiguration config) {
		super("biz.aQute.osgi.logger.rolling " + config.to());
		this.context = context;
		this.lrs = lrs;
		this.formatter = new LogMessageFormatter(config.format());

		String fp = config.to();
		if (fp == null || fp.isEmpty()) {
			root = null;
			return;
		}

		File root;
		File f = new File(fp);
		if (f.isAbsolute()) {
			root = f;
		} else {
			root = context.getDataFile(f.getPath());
			if (root == null) {
				throw new IllegalStateException("File system support is not available, cannot create file object");
			}
		}
		root.mkdirs();
		if (!root.isDirectory()) {
			throw new IllegalStateException("Cannot create directory " + root);
		}

		this.root = root;

		lrs.addLogListener(this::log);
		queue.addAll(Collections.list(lrs.getLog()));
		start();
	}

	/*
	 * We cannot be reconfigured. We do not want to restart for not losing log
	 * messages but to reconfigure is then tricky and not worth the effort
	 */
	@Modified
	void modified(RollingLogReaderConfiguration config) {}

	/**
	 * Called when this service instance has been deactivated
	 */
	@Deactivate
	void deactivate() throws InterruptedException {
		interrupt();
		try {
			join(MAX_WAIT_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread()
				.interrupt();
			return;
		}
	}

	/**
	 * Overrides the Thread run method
	 *
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try {

			File logFile = new File(root, CURRENT_TXT);
			if (logFile.isFile() && !logFile.canWrite()) {
				System.err.println("cannot write log file " + logFile + ", quiting");
				return;
			}
			while (true) {
				long flush = System.currentTimeMillis() + Integer.MAX_VALUE;

				try (FileChannel out = FileChannel.open(logFile.toPath(), StandardOpenOption.APPEND,
					StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
					ByteBuffer buffer = ByteBuffer.allocate(MAX_ENTRY_SIZE);

					while (logFile.length() < limit) {
						LogEntry entry = queue.poll(1, TimeUnit.SECONDS);
						if (entry != null) {
							buffer.clear();
							byte[] data = formatter.format(entry)
								.getBytes(StandardCharsets.UTF_8);
							buffer.put(data, 0, Math.min(MAX_ENTRY_SIZE - 10, data.length));
							buffer.put((byte) '\n');
							buffer.flip();
							out.write(buffer);
							flush = System.currentTimeMillis() + MAX_STALE_TIME_MILLIS;
						}

						long now = System.currentTimeMillis();
						if (flush < now) {
							out.force(false);
							flush = System.currentTimeMillis() + Integer.MAX_VALUE;
						}
						nextEntry++;
					}

					out.close();
					rollover(logFile);
					nextFile++;
				}
			}
		} catch (InterruptedException e) {
			interrupt();
			return;
		} catch (Exception e) {
			try {
				e.printStackTrace();
				Thread.sleep(EXCEPTION_PAUSE_MILLIS);
			} catch (InterruptedException e1) {
				interrupt();
				return;
			}
		}
	}

	private void rollover(File logFile) throws IOException {
		if (root == null)
			return;

		String name = FILENAME_FORMATTER.format(clock.instant())
			.concat(".txt");
		File to = IO.getFile(root, name);
		Files.move(logFile.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);

		purge();
	}

	/**
	 * Gets the list of persistent log files
	 *
	 * @return array of sorted log files
	 */
	public List<File> logfiles() {
		if (root == null)
			return Collections.emptyList();

		File[] logFiles = root.listFiles(s -> s.getName()
			.matches(FILENAME_MATCHER));

		List<File> result = new ArrayList<>(Arrays.asList(logFiles));
		Collections.sort(result, (a, b) -> a.getName()
			.compareTo(b.getName()));
		result.add(new File(root, CURRENT_TXT));
		return result;
	}

	/**
	 * Search the log
	 */

	public CharSequence find(@Parameter(absentValue = "false", presentValue = "true", names = {
		"-c", "--current"
	}) boolean currentOnly, String globString) {
		Predicate<String> predicate = new Glob(globString).pattern()
			.asPredicate();
		StringBuilder sb = new StringBuilder();
		System.out.println(globString);

		logfiles().stream()
			.filter(f -> !currentOnly || f.getName()
				.equals(CURRENT_TXT))
			.map(File::toPath)
			.map(p -> {
				System.out.println(p);
				return p;
			})
			.flatMap(this::lines)
			.map(l -> {
				System.out.println(l);
				return l;
			})
			.filter(predicate)
			.forEach(sb::append);

		return sb;
	}

	private Stream<String> lines(Path path) {
		try {
			return Files.lines(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void flush(int n) {
		List<LogEntry> flushed = new ArrayList<>();
		queue.drainTo(flushed, n);
		System.err.println(Strings.join("\n", flushed));
	}

	private void purge() {
		List<File> files = logfiles();

		while (files.size() > 10) {
			File file = files.remove(0);
			IO.delete(file);
			if (file.isFile())
				System.err.println("Cannot delete log file " + file);
		}
	}

	void log(LogEntry e) {

		if (queue.size() >= MAX_QUEUE_LENGTH / 2) {
			try {
				// Throttle logging rate to give the log thread a chance to keep
				// up
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				Thread.currentThread()
					.interrupt();
				return;
			}
		}

		while (!queue.offer(e)) {
			System.err.println("Appender logging queue to " + root + " is full. Flushing to System.err");
			flush(MAX_QUEUE_LENGTH / 10);
		}
	}

}
