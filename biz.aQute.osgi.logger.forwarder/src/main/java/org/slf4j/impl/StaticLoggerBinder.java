package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

import biz.aQute.osgi.logger.forwarder.LogForwarder;

public class StaticLoggerBinder implements LoggerFactoryBinder {

	public /* final */ static String		REQUESTED_API_VERSION	= "1.7.30";
	private static final StaticLoggerBinder	SINGLETON;

	static {
		SINGLETON = new StaticLoggerBinder();
	}

	public static final StaticLoggerBinder getSingleton() {
		return SINGLETON;
	}

	@Override
	public ILoggerFactory getLoggerFactory() {
		return LogForwarder.getLoggerFactory();
	}

	@Override
	public String getLoggerFactoryClassStr() {
		return LogForwarder.class.getName();
	}

}
