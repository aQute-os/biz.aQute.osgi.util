package biz.aQute.scheduler.basic.provider.example;

import java.util.Date;

import org.osgi.service.component.annotations.Component;

import biz.aQute.scheduler.api.CronJob;
import biz.aQute.scheduler.api.annotation.CronExpression;

@Component(
		service = CronJob.class, // optional
		immediate = true) // optional
@CronExpression // takes default cron
public class ExampleJobSecond implements CronJob<Void> {

	@Override
	public void run(Void data) throws Exception {
		System.out.println(ExampleJobSecond.class.getSimpleName() + " executed at: " + new Date());
	}

}
