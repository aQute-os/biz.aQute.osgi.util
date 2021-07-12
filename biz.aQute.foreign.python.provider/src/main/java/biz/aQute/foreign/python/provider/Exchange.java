package biz.aQute.foreign.python.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

public class Exchange extends InputStream implements Appendable {
	final static int	MASK		= 0x1FFF;
	char				buffer[]	= new char[MASK + 1];
	int					available	= buffer.length;
	int					in, out;
	int					count		= 0;
	int					codepoint;
	final String		name;

	public Exchange(String name) {
		this.name = name;
	}

	@Override
	public Appendable append(CharSequence csq) throws IOException {
		return append(csq, 0, csq.length());
	}

	@Override
	public Appendable append(CharSequence csq, int start, int end) throws IOException {
		for (int i = start; i < end; i++)
			append(csq.charAt(i));
		return this;
	}

	@Override
	public synchronized Appendable append(char c) throws IOException {
		while (available == 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}
		available--;
		buffer[in] = c;
		in = (in + 1) & MASK;
		notifyAll();
		return this;
	}

	public synchronized char readChar() throws InterruptedIOException {
		while (in == out) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}
		char c = buffer[out];
		out = (out + 1) & MASK;
		available++;
		return c;
	}

	@Override
	public int read() throws IOException {
		if (count-- > 0) {
			int r = codepoint >> (count * 6);
			r &= 0b0011_1111;
			r |= 0b1000_0000;
			return r;
		}

		codepoint = readChar();
		if (codepoint < 0)
			return -1;
		if (codepoint > 0xFFFF)
			return 0;

		if (codepoint < 0x80)
			return codepoint;

		if (codepoint < 0x800) {
			count = 1;
			int v = (codepoint >> 6) | 0b1100_0000;
			return v;
		}
		count = 2;
		int v = (codepoint >> 12) | 0b1110_0000;
		return v;
	}

	@Override
	public synchronized int available() {
		return available;
	}

	@Override
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder(name).append(": ");
		int i=out;
		while ( i!=out) {
			sb.append(buffer[i]);
			i = (i+1) & MASK;
		}
		return sb.toString();
	}
}
