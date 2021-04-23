package biz.aQute.shell.sshd.provider;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Inserts a CR before a LF if it is missing. It also buffers the output until a flush is seen
 */
public class OutputWrapper extends OutputStream {

	private static final byte	LF		= '\n';
	private static final byte	CR		= '\r';

	final OutputStream			out;
	byte						lastChar;
	byte[]						buffer	= new byte[50000];
	int							insert	= 0;

	public OutputWrapper(OutputStream out) {
		this.out = out;
	}

	@Override
	public void write(int b) throws IOException {
		synchronized (buffer) {
			push(b);
		}
	}

	public void write(byte buffer[], int off, int len) throws IOException {
		synchronized (buffer) {
			for (int i = 0; i < len; i++) {
				push(buffer[i + off]);
			}
		}
	}

	public void flush() throws IOException {
		synchronized (buffer) {
			flush0();
		}
	}

	private void flush0() throws IOException {
		out.write(buffer, 0, insert);
		insert = 0;
		out.flush();
	}

	private void push(int c) throws IOException {
		if (  c == LF) {
			if ( lastChar != CR)
				store(CR);
		}
		store(c);
	}

	private void store(int c) throws IOException {
		if (insert >= buffer.length) {
			flush0();
		}
		lastChar = buffer[insert++] = (byte) (0xFF & c);
	}

}
