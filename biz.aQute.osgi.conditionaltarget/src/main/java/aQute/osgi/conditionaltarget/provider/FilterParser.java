package aQute.osgi.conditionaltarget.provider;

import java.util.Collections;
import java.util.Set;

import aQute.lib.collections.MultiMap;

/**
 * Simple parser to extract the keys from a filter
 */
class FilterParser {
	final MultiMap<String, String>	map	= new MultiMap<>();
	final String					filter;
	int								n;

	FilterParser(String filter) {
		this.filter = filter;
	}

	void filter() {
		expect("(", "Expect '('");
		char c = peek(1);
		switch (c) {
		case 0:
			throw new IllegalArgumentException("Unexpected end of filter");
		case '|':
		case '&':
			subexpression();
			break;

		case '!':
			not();
			break;

		default:
			simple();
		}
		expect(")", "Expects ')'");
		n++;
	}

	private void not() {
		n+=2;
		filter();
	}

	private void simple() {
		expect("(", "Expect '('");
		n++;
		StringBuilder attr = new StringBuilder();
		@SuppressWarnings("unused")
		String op;
		attr: while (true) {
			switch (current()) {
			case 0:
				throw new IllegalArgumentException("Unexpected end of string");

			case '=':
				if (peek(1) == '*') {
					op = "=*";
				} else
					op = "=";
				n++;
				break attr;

			case '>':
			case '<':
			case '~':
				if (peek(1) == '=') {
					op = current() + "=";
					n++;
				} else {
					throw new IllegalArgumentException("The >,<,~ operators must be followed by an '=': " + n);
				}
				n++;
				break attr;
			}
			attr.append(current());
			n++;
		}

		StringBuilder value = new StringBuilder();
		while ("()".indexOf(current()) < 0) {
			if ( current() == 0)
				throw new IllegalArgumentException("Unexpected end of string");

			if (current() == '\\') {
				n++;
			}
			value.append(current());
			n++;
		}
		map.add(attr.toString().trim(), value.toString());
	}

	private void subexpression() {
		expect("(", "Expect '('");
		n+=2;
		do {
			filter();
		} while (current() == '(');
	}

	private char current() {
		return peek(0);
	}

	private void expect(String expect, String message) {
		for (int i = 0; i < expect.length(); i++) {
			if (peek(i) != expect.charAt(i))
				throw new IllegalArgumentException(message + " at " + n);
		}
	}

	private char peek(int i) {
		int nn = n + i;
		if (nn >= filter.length())
			return 0;

		return filter.charAt(nn);
	}

	public static Set<String> getAttributes(String filter) {
		if ( filter == null) {
			return Collections.emptySet();
		}
		FilterParser p = new FilterParser(filter);
		p.filter();
		return p.map.keySet();

	}

}
