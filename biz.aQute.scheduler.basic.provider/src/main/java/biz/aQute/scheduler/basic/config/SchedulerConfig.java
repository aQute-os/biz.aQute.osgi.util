package biz.aQute.scheduler.basic.config;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for the scheduler. The scheduler is a singleton
 */

@ObjectClassDefinition
public @interface SchedulerConfig {
	String PID = "biz.aQute.scheduler.basic";
	String SYSTEM_DEFAULT_TIMEZONE = "system.default.timezone";

	/**
	 * Set the time zone. The default is the system default time zone. If the
	 * time zone does not exist, an error is logged and the system default time zone is
	 * used.
	 */
	String timeZone() default SYSTEM_DEFAULT_TIMEZONE;
}