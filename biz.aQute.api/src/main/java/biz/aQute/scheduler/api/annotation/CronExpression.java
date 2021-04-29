package biz.aQute.scheduler.api.annotation;

import org.osgi.service.component.annotations.ComponentPropertyType;

@ComponentPropertyType
public @interface CronExpression {

	public static final String PROPERTY_NAME = "cron.expression";


	String value() default "0 * * ? * *"; // every minute

}
