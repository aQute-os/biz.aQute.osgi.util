package org.slf4j.impl;

public enum Level {
	TRACE,
	DEBUG,
	INFO,
	WARN,
	ERROR;

	public boolean implies(Level l) {
		return this.ordinal() >= l.ordinal();
	}
}
