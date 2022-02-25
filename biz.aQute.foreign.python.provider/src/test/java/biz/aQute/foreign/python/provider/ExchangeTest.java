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
	public void testBlockOutput() throws IOException, InterruptedException {
		try (Exchange e = new Exchange("test");) {
			byte[] data = new byte[e.buffer.length - 1];
			for (int i = 0; i < data.length; i++) {
				data[i] = (byte) ('0' + (i % 10));
			}
			String s = new String(data, StandardCharsets.US_ASCII);
			e.append(s);
			e.append('X');
			CompletableFuture<Void> f = bg(() -> {
				System.out.println("blocking on append");
				e.append('Y');
				System.out.println("finished blocking on append");
			});
			Thread.sleep(100);
			assertThat(e.freespace).isEqualTo(0);
			assertThat(f.isDone()).isFalse();

			e.read();
			Thread.sleep(100);
			assertThat(e.freespace).isEqualTo(0);
			assertThat(f.isDone()).isTrue();
		}
	}

	@Test
	public void testBlockInput() throws IOException, InterruptedException {
		try (Exchange e = new Exchange("test");) {
			CompletableFuture<Void> f = bg(() -> {
				System.out.println("blocking on read");
				e.read();
				System.out.println("finished blocking on read");
			});
			Thread.sleep(100);
			assertThat(e.freespace).isEqualTo(e.buffer.length);
			assertThat(f.isDone()).isFalse();

			e.append('X');
			Thread.sleep(100);
			assertThat(e.freespace).isEqualTo(e.buffer.length);
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
