package biz.aQute.scheduler.basic.provider.example;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import biz.aQute.scheduler.api.Scheduler;

@Component(
		immediate = true)
public class ExampleSchedulerClient {

	@Reference
	private Scheduler scheduler;

	private final Logger logger = org.slf4j.LoggerFactory.getLogger(ExampleSchedulerClient.class);

	private final String cronExpression = "* * * * * ? *"; // or "@daily" etc., for more shorthands see 'CronAdjuster'

	private Closeable c;


	@Activate
	private void activate() throws Exception {
		c = scheduler.schedule(() -> logger.info("Hello at: " + new Date()), cronExpression);
	}


	@Deactivate
	private void deActivate() throws IOException {
		c.close();
	}

}
