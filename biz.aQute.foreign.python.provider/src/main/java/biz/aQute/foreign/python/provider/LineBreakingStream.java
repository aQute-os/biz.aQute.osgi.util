package biz.aQute.foreign.python.provider;

import java.io.IOException;
import java.io.PrintStream;

public class LineBreakingStream implements Appendable {
	StringBuilder	buffer	= new StringBuilder();
	int				n		= 0;
	PrintStream		out;

	public LineBreakingStream(PrintStream out) {
		this.out = out;
	}

	@Override
	public Appendable append(CharSequence csq) throws IOException {
		return append(csq, 0, csq.length());
	}

	@Override
	public synchronized Appendable append(CharSequence csq, int start, int end) throws IOException {
		for (int i = start; i < end; i++) {
			char c = csq.charAt(i);
			add0(c);
		}
		return null;
	}

	@Override
	public synchronized Appendable append(char c) throws IOException {
		add0(c);
		return this;
	}

	private void add0(char c) {
		buffer.append(c);
		if (c == '\n' || buffer.length() > 4000) {
			out.append(buffer);
			out.flush();
			buffer.setLength(0);
		}
	}

	public void flush() {
		if (buffer.length() > 0) {
			out.append(buffer);
			out.flush();
			buffer.setLength(0);
		}
	}

}
