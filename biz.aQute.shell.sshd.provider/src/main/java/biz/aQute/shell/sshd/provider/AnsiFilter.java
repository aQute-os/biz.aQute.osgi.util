package biz.aQute.shell.sshd.provider;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import aQute.lib.io.IO;
import aQute.libg.glob.Glob;

public class AnsiFilter {
	final static byte[]		crlf	= new byte[] { 10, 13 };

	static final int		BS		= 0x08;
	final static char		ESCAPE	= 0x1B;
	final static char		DEL		= 0x7F;
	final InputStream		in;
	final OutputStream		out;
	final Writer			writer;
	final BufferedReader	reader;
	final Charset			charset;
	final int				w;
	final int				h;
	final String			term;
	final List<String>		history	= new ArrayList<>();
	final boolean			ansi;
	int						where	= 0;
	StringBuilder			buffer;
	int						cursor	= 0;
	int						start	= 0;
	String					current	= "";
	boolean					hex;
	boolean					cr=true;
	boolean					echo=true;

	public AnsiFilter(int w, int h, InputStream in, OutputStream out, String term, Charset charset) {
		this.w = w;
		this.h = h;
		this.in = in;
		this.term = term;
		this.ansi = !"none".equalsIgnoreCase(term);

		OutputStream tout = out;
//		if (ansi) {
//			tout = new FilterOutputStream(out) {
//				public void write(int b) throws IOException {
//					if (b == '\n' && cr) {
//						out.write(crlf);
//						out.flush();
//					} else if(b=='\r') {
//						//skip
//					}
//					else {
//						out.write(b);
//					}
//				}
//			};
//		}
		this.out = tout;
		this.charset = charset;
		this.reader = IO.reader(this.in, charset);
		this.writer = new OutputStreamWriter(tout);
	}

	public String readline(String prompt) throws IOException {
		if (!ansi)
			return reader.readLine();

		outer: while (true) {
			writer.write(prompt);
			writer.flush();
			start = prompt.length();
			buffer = new StringBuilder();
			current = "";
			cursor = 0;

			while (true) {
				int c = reader.read();
				if (c < 0)
					throw new EOFException();

				if (hex) {
					System.err.printf("%02X ", c);
				}

				switch (c) {
				case '\n': // enter;
				case '\r': // enter;
					current = buffer.toString();
					if (current.startsWith("!")) {

						Glob g = new Glob(current.substring(1));
						Optional<String> match = history.stream().filter(s -> g.pattern().matcher(s).lookingAt())
								.findFirst();
						if (!match.isPresent()) {
							write("\n" + g + " not found in history\n");
							continue outer;
						}
						current = match.get();
					}
					if (current.trim().length() > 0)
						history.add(0, current);
					return current;

				case 0x01:
					cursor = 0;
					rewrite();
					break;

				case 0x02:
					hex = !hex;
					break;

				case 0x03:
					buffer.setLength(0);
					cursor = 0;
					rewrite();
					break;

				case 0x04:
					return null;

				case 0x05:
					cursor = buffer.length();
					rewrite();
					break;

				case 0x7F:
				case BS:
					if (cursor > 0) {
						buffer.delete(cursor - 1, cursor);
						cursor--;
						rewrite();
					}
					break;

				case ESCAPE:
					escape();
					break;

				default:
					if (Character.isJavaIdentifierPart(c) || (c >= 0x20 && c < 0x7F)) {
						buffer.insert(cursor, (char) c);
						cursor++;
						rewrite();
					}
					break;

				}
			}
		}
	}

	private void delete() throws IOException {
		if (buffer.length() > cursor) {
			buffer.delete(cursor, cursor + 1);
			rewrite();
		}
	}

	void escape() throws IOException {
		int c = reader.read();
		if (c == '[') {
			c = reader.read();
			switch (c) {
			case 'H':
				cursor = 0;
				rewrite();
				break;

			case 'C':// right
				if (cursor < buffer.length()) {
					cursor++;
					rewrite();
				}
				break;

			case 'D':
				if (cursor > 0) {
					cursor--;
					rewrite();
				}
				break;
			case 'A':// up
				if (where < history.size()) {
					buffer = new StringBuilder(history.get(where));
					where++;
					cursor = buffer.length();
					rewrite();
				}
				break;

			case 'B': // down
				if (where > 0) {
					where--;
					buffer = new StringBuilder(history.get(where));
					cursor = buffer.length();
					rewrite();
				}
				break;

			case '3':
				c = reader.read();
				switch (c) {
				case '~':
					delete();
				}
				break;

			default:
				break;
			}
		}
		// ESC[H moves cursor to home position (0, 0)
		// ESC[{line};{column}H
		// ESC[{line};{column}f moves cursor to line #, column #
		// ESC[#A moves cursor up # lines
		// ESC[#B moves cursor down # lines
		// ESC[#C moves cursor right # columns
		// ESC[#D moves cursor left # columns
		// ESC[#E moves cursor to beginning of next line, # lines down
		// ESC[#F moves cursor to beginning of previous line, # lines down
		// ESC[#G moves cursor to column #
		// ESC[#;#R reports current cursor line and column
		// ESC[s saves the current cursor position
		// ESC[u restores the cursor to the last saved position
		// Erase Functions
		// ESC Code Sequence Description
		// ESC[J clears the screen
		// ESC[0J clears from cursor until end of screen
		// ESC[1J clears from cursor to beginning of screen
		// ESC[2J clears entire screen
		// ESC[K clears the current line
		// ESC[0K clears from cursor to end of line
		// ESC[1K clears from cursor to start of line
		// ESC[2K clears entire line
		// Colors / Graphics Mode
		// ESC Code Sequence Description
		// ESC[{...}m Set styles and colors for cell and onward.
		// ESC[0m reset all styles and colors
		// ESC[1m set style to bold
		// ESC[2m set style to dim
		// ESC[2m set style to dim
	}

	private void rewrite() throws IOException {
		if (!echo)
			return;
		int i = 0;
		while (i < this.current.length() && i < buffer.length() && this.current.charAt(i) == this.buffer.charAt(i))
			i++;

		column(start+i+1);

		String current = buffer.toString();
		writer.write(current, i, current.length() - i);
		if (this.current.length() > current.length())
			eraseEOL();

		if (cursor < current.length()) {
			left(current.length() - cursor);
		}

		this.current = current;
		writer.flush();
	}

	private void column(int i) throws IOException {
		write("\u001B["+i+"G");
	}

	private void eraseEOL() throws IOException {
		write("\u001B[0K");
	}

//	private void begin() throws IOException {
//		write("\r");
//	}
//
//	private void right(int cnt) throws IOException {
//		write("\u001B[" + cnt + "C");
//	}

	private void left(int cnt) throws IOException {
		write("\u001B[" + cnt + "D");
	}

	public void write(String s) throws IOException {
		writer.write(s);
	}

	public void flush() throws IOException {
		writer.flush();
	}

	public List<String> getHistory() {
		return history;
	}

	public void setNoCR() {
		cr = false;
	}

	public void setNoEcho() {
		echo = false;
	}

	public void writeln() throws IOException {
		writer.write("\r\n");
		writer.flush();
	}

}
