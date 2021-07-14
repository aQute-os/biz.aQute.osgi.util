package biz.aQute.foreign.python.configuration;

public @interface Configuration {
	String PID = "biz.aQute.foreign.python";

	String python() default "python3";
	long restartDelay() default 5000;
}
