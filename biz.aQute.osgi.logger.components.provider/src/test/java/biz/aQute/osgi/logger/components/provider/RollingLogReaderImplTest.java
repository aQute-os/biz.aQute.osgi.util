package biz.aQute.osgi.logger.components.provider;


import static biz.aQute.osgi.logger.components.provider.Mocks.await;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;

import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import biz.aQute.osgi.logger.components.config.RollingLogReaderConfiguration;

public class RollingLogReaderImplTest {
	final Mocks				m		= new Mocks();
	long					time	= 0;
	@Rule
	public TemporaryFolder	tmp		= new TemporaryFolder();

	@Test
	public void test() throws Exception {
		File root = tmp.newFolder();
		File current = IO.getFile(root, "current.txt");

		RollingLogReaderImpl rli = new RollingLogReaderImpl(m.getContext(), m.getLogReaderService(),
			m.getMap(RollingLogReaderConfiguration.class, "to", root.getAbsolutePath()));

		try {
			LogEntry entry = m.entry("hello world", LogLevel.WARN, "foo");
			int e1 = rli.nextEntry;
			rli.log(entry);

			Mocks.await(5000, () -> rli.nextEntry != e1, "waiting for first entry");

			assertThat(current).hasContent("01/01 00:00:00 WARN   [foo] hello world");

			assertThat(rli.logfiles()).contains(new File(root, "current.txt"))
				.hasSize(1);

			rli.limit = 1;


			int e2 = rli.nextFile;
			log(rli, "should create a rollover", "foo");
			Mocks.await(5000000, () -> rli.nextFile != e2, "waiting for next entry");

			List<File> logfiles = rli.logfiles();
			assertThat(logfiles).hasSize(2)
				.contains(new File(root, RollingLogReaderImpl.CURRENT_TXT))
				.contains(new File(root, "19700101T010000.txt"));

			for (int i = 0; i < 100; i++) {
				log(rli, "index " + Integer.toString(i), "foo");
			}

			await(5000, () -> current.isFile());

			logfiles = rli.logfiles();
			assertThat(logfiles).hasSize(10)
				.contains(current);

			CharSequence all = rli.find(false, "FOOBAR");
			assertThat(all).isEmpty();

			log(rli, "FOOBAR", "NAME");

			all = rli.find(false, "FOOBAR");
			assertThat(all).isNotEmpty();

		} finally {
			rli.deactivate();
		}
	}

	@Test
	public void testLargeLogMessage() throws Exception {
		File root = tmp.newFolder();
		File current = IO.getFile(root, "current.txt");

		RollingLogReaderImpl rli = new RollingLogReaderImpl(m.getContext(), m.getLogReaderService(),
			m.getMap(RollingLogReaderConfiguration.class, "to", root.getAbsolutePath()));

		try {
			String large = Strings.times("Foo Bar\n", 5000);
			LogEntry entry = m.entry("hello world", LogLevel.WARN, large);

			assertThat(current.length()).isEqualTo(0);

			int e1 = rli.nextEntry;
			rli.log(entry);

			Mocks.await(1000, () -> rli.nextEntry != e1, "waiting for first entry");

			String content = IO.collect(new File(root, "current.txt"));
			assertThat(content.length()).isLessThan(RollingLogReaderImpl.MAX_ENTRY_SIZE)
				.isGreaterThan(RollingLogReaderImpl.MAX_ENTRY_SIZE - 100);


		} finally {
			rli.deactivate();
		}
	}

	private void log(RollingLogReaderImpl rli, String message, String name) {
		LogEntry entry;
		int e2 = rli.nextEntry;
		time += 3_600_000;
		rli.clock = Mocks.getClock(time);
		entry = m.entry(message, LogLevel.WARN, name);
		rli.log(entry);
		await(1000, () -> rli.nextEntry != e2);
	}

}
