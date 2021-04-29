package biz.aQute.scheduler.basic.provider.example;

import java.util.Date;

import org.osgi.service.component.annotations.Component;

import biz.aQute.scheduler.api.CronJob;
import biz.aQute.scheduler.api.annotation.CronExpression;


@Component
@CronExpression("* * * * * ? *")
public class ExampleJob implements CronJob<Object> {

	@Override
	public void run(Object s) throws Exception {
		System.out.println(ExampleJob.class.getSimpleName() + " executed at: " + new Date());
	}

}
