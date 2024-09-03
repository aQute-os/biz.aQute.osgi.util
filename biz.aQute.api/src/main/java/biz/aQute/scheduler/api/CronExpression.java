package biz.aQute.scheduler.api;

import org.osgi.service.component.annotations.ComponentPropertyType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * An annotation to simplify using a CronJob
 */
@ComponentPropertyType
@ObjectClassDefinition
@RequireSchedulerImplementation
public @interface CronExpression {

	public static final String PREFIX_ = Constants.SERVICE_PROPERTY_CRONJOB_PREFIX;
	/**
	 * The 'cronjob.cron' service property as defines in {@link Constants#SERVICE_PROPERTY_CRONJOB_CRON}
	 * @return
	 */
	@AttributeDefinition(name="cronJobCronExpression", description = "Cron Expression according the Cron Spec. see http://en.wikipedia.org/wiki/Cron")
	String[] cron();
	
	/**
	 * The 'cronjob.name' service property as defines in {@link Constants#SERVIC_PROPERTY_CRON_NAME()()}
	 * @return
	 */
	@AttributeDefinition(name="cronJobName", description = "Human readable name of the cronjob")
	String name() default Constants.CRONJOB_NAME_DEFAULT;


}

