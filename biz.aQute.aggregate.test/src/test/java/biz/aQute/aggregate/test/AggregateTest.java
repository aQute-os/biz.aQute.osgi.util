package biz.aQute.aggregate.test;

import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Constants;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import biz.aQute.aggregate.api.Aggregate;

public class AggregateTest {
	static {
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "debug");
	}

	static LaunchpadBuilder					builder	= new LaunchpadBuilder().nostart()
		.bndrun("test.bndrun")
		.set(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "2");
	
	interface T1Agg extends Aggregate<String> {}
	
	
@Ignore	
	@Test
	public void simple() throws Exception {
		try (Launchpad lp = builder.create()) {
			lp.start();

			System.out.println();
		}

	}

}
