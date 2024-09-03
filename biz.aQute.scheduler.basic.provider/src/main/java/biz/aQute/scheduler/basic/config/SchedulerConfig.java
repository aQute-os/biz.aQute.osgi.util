package biz.aQute.scheduler.basic.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( description = "Configuration for the scheduler. The scheduler is a singleton")
public @interface SchedulerConfig {
	String PID = "biz.aQute.scheduler.basic";
	String SYSTEM_DEFAULT_TIMEZONE = "system.default.timezone";
	String DESCRIPTION = "Set the time zone. The default is the system default time zone. "
			+ "If the time zone does not exist, an error is logged and the system default time zone is used.";
	int COREPOOLSIZE_DEFAUL = 50;
	int SHUTDOWNTIMEOUT_SOFT_DEFAUL = 500;
	int SHUTDOWNTIMEOUT_HARD_DEFAUL = 5000;

	@AttributeDefinition(description = DESCRIPTION)
	String timeZone() default SYSTEM_DEFAULT_TIMEZONE;
	
	@AttributeDefinition(description = "corePoolSize of ScheduledThreadPoolExecutor")
	int corePoolSize() default COREPOOLSIZE_DEFAUL;
	
	@AttributeDefinition(description = "Expected shutdownTimeout of ScheduledThreadPoolExecutor")
	int shutdownTimeoutSoft() default SHUTDOWNTIMEOUT_SOFT_DEFAUL;
	
	@AttributeDefinition(description = "Expected shutdownTimeout after the shutdownTimeoutSoft befor the shutdown of ScheduledThreadPoolExecutor is forced")
	int shutdownTimeoutHard() default SHUTDOWNTIMEOUT_HARD_DEFAUL;
}