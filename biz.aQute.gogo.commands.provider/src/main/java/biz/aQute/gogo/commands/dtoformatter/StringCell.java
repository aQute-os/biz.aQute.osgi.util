package biz.aQute.gogo.commands.dtoformatter;

import aQute.lib.justif.Justif;

public class StringCell implements Cell {
	static Justif			j	= new Justif(80);

	public final String[]	value;
	public final int		width;
	final Object			original;

	public StringCell(String label, Object original) {
		this.original = original;
		this.value = label.split("\\s*\r?\n");
		int w = 0;
		for (String l : value) {
			if (l.length() > w)
				w = l.length();
		}
		this.width = w;
	}

	public StringCell(String[] array, Object original) {
		this.value = array;
		this.original = original;
		int w = 0;
		for (String l : value) {
			if (l.length() > w)
				w = l.length();
		}
		this.width = w;
	}

	@Override
	public int width() {
		return width + 2;
	}

	@Override
	public int height() {
		return value.length + 2;
	}

	@Override
	public String toString() {
		return String.join("\n", value);
	}

	@Override
	public Object original() {
		return original;
	}

}
