package biz.aQute.foreign.python.configuration;

public @interface Configuration {
	String python() default "python3";
	long restartDelay() default 5000;
}
