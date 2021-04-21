package biz.aQute.shell.sshd.provider;

import java.io.IOException;
import java.io.OutputStream;


/**
 * Inserts a CR before a LF if it is missing
 */
public class OutputWrapper extends OutputStream {

	private static final byte	LF		= '\n';
	private static final byte	CR		= '\r';
	private static final byte[]	CRLF	= { CR, LF };

	final OutputStream			out;
	byte						lastChar;

	public OutputWrapper(OutputStream out) {
		this.out = out;
	}

	@Override
	public void write(int b) throws IOException {
		synchronized (this) {
			if (b == LF) {
				if (lastChar != CR)
					out.write(CRLF, 0, 2);
				else
					out.write(LF);
			} else {
				out.write(b);
			}
			lastChar = (byte) b;
		}
	}

	public void write(byte buffer[], int off, int len) throws IOException {
		synchronized (this) {
			for (int i = 0; i < len; i++) {
				byte b = buffer[i];
				if (b == LF) {
					if (lastChar != CR)
						out.write(CRLF, 0, 2);
					else
						out.write(LF);
				} else {
					out.write(b);
				}
				lastChar = (byte) b;
			}
		}
	}

	public void flush() throws IOException {
		out.flush();
	}

}
