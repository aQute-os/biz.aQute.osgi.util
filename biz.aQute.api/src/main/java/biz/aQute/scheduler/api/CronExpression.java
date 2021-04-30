package biz.aQute.scheduler.api;

import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * An annotation to simplify using a CronJob
 */
@ComponentPropertyType
public @interface CronExpression {

	/**
	 * The 'cron.expression' service property
	 * @return
	 */
	String cron();

}

