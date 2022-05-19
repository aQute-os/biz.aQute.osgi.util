package biz.aQute.foreign.python.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import aQute.lib.exceptions.RunnableWithException;
import aQute.lib.io.IO;

public class ExchangeTest {

	@Test
	public void testAvailable() throws IOException, InterruptedException {
		try (Exchange e = new Exchange("test");) {
			assertThat(e.available()).isEqualTo(0);
			CompletableFuture<Void> f = bg(() -> {
				System.out.println("append A");
				e.append('A');
				System.out.println("append B");
				e.append('B');
				System.out.println("done");
			});

			System.out.println("wait until 2 chars inserted");
			while (e.available() != 2) {
				System.out.println(e.available());
				Thread.sleep(10);
			}

			assertThat(e.read()).isEqualTo('A');
			assertThat(e.read()).isEqualTo('B');

			CompletableFuture<Void> ff = bg(() -> {
				System.out.println("append A's");
				for (int i = 0; i < e.buffer.length + 1; i++) {
					e.append('A');
				}
				System.out.println("done append A's");
			});
			Thread.sleep(100);
			System.out.println("we should have the full buffer available");
			assertThat(e.available()).isEqualTo(e.buffer.length);

			System.out.println("we wrote 1 char more, so if we read one, we should still have the buffer full");
			e.read();
			Thread.sleep(100);
			assertThat(e.available()).isEqualTo(e.buffer.length);
		}

	}

	@Test
	public void testBlockOutput() throws IOException, InterruptedException {
		try (Exchange e = new Exchange("test");) {
			System.out.println("creating buffer that is one less the buffer size ");
			byte[] data = new byte[e.buffer.length - 1];
			for (int i = 0; i < data.length; i++) {
				data[i] = (byte) ('0' + (i % 10));
			}
			String s = new String(data, StandardCharsets.US_ASCII);
			e.append(s);
			System.out.println("Now send 1 character that fills up our buffer. So blocking next");
			e.append('X');
			CompletableFuture<Void> f = bg(() -> {
				System.out.println("blocking on append now");
				e.append('Y');
				System.out.println("finished blocking on append");
			});
			Thread.sleep(100);
			System.out.println("We should have no freespace");
			assertThat(e.freespace).isEqualTo(0);
			assertThat(e.available() == e.buffer.length);
			System.out.println("And our background should not be finished");
			assertThat(f.isDone()).isFalse();

			System.out.println("Reading this will release the background");
			e.read();
			Thread.sleep(100);
			System.out.println("But it will append its char, so freespace should still be 0");
			assertThat(e.freespace).isEqualTo(0);
			System.out.println("But our bg is released");
			assertThat(f.isDone()).isTrue();
		}
	}

	@Test
	public void testBlockInput() throws IOException, InterruptedException {
		try (Exchange e = new Exchange("test");) {

			System.out.println("Block on input of a single character");
			CompletableFuture<Void> f = bg(() -> {
				System.out.println("blocking on read");
				e.read();
				System.out.println("finished blocking on read");
			});
			Thread.sleep(100);
			System.out.println("We should have the full buffer available");
			assertThat(e.freespace).isEqualTo(e.buffer.length);
			System.out.println("And still blocking");
			assertThat(f.isDone()).isFalse();

			System.out.println("Sending one character, releasing the bg");
			e.append('X');
			Thread.sleep(100);
			System.out.println("Still full buffer because ew read the character");
			assertThat(e.freespace).isEqualTo(e.buffer.length);
			System.out.println("But the bg is released");
			assertThat(f.isDone()).isTrue();
		}
	}

	@Test
	public void testWriterIsFaster() throws IOException, InterruptedException, ExecutionException {
		try (Exchange e = new Exchange("test");) {
			CompletableFuture<Void> f = bg(() -> {
				System.out.println("blocking on read");
				int n = 0;
				while (true) {
					int read = e.read();
					if ((n++ % 10) == 0)
						Thread.sleep(1);
					if (read == 'X')
						break;
				}
				System.out.println("finished blocking on read");
			});

			for (int i = 0; i < 10_000; i++) {
				e.append("Hello world");
				if (i == 5000) {
					Thread.sleep(1000);
				}
			}
			e.append('X');
			System.out.println(" read block " + e.readBlocks);
			System.out.println(" write block " + e.writeBlocks);
			f.get();

		}
	}

	@Test
	public void testReaderIsFaster() throws IOException, InterruptedException, ExecutionException {
		try (Exchange e = new Exchange("test");) {
			CompletableFuture<Void> f = bg(() -> {
				System.out.println("blocking on write");
				for (int i = 0; i < 10_000; i++) {
					Thread.sleep(1);
					e.append("0123456789");
				}
				e.append('X');
				System.out.println("finished blocking on write");
			});
			int n = 0;
			while (true) {
				int read = e.read();
				if (read == 'X')
					break;
				n++;
			}
			assertThat(n).isEqualTo(100_000);
			System.out.println(" read block " + e.readBlocks);
			System.out.println(" write block " + e.writeBlocks);
			f.get();

		}
	}

	@Test
	public void testUTF8() throws IOException {
		String s = IO.collect("resources/unicode-test-file.txt")
			.concat("\u0000");
		try (Exchange e = new Exchange("test");) {
			CompletableFuture<Void> f = bg(() -> {
				System.out.println("blocking on write");
				e.append(s);
				System.out.println("finished blocking on write");
			});

			StringBuilder sb = new StringBuilder();
			while (true) {
				char c = e.readChar();
				sb.append(c);
				if (c == '\u0000')
					break;

			}
			assertThat(s).isEqualTo(sb.toString());
			System.out.println(" read block " + e.readBlocks);
			System.out.println(" write block " + e.writeBlocks);
		}

	}

	private CompletableFuture<Void> bg(RunnableWithException cb) {
		return CompletableFuture.runAsync(() -> {
			try {
				cb.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
