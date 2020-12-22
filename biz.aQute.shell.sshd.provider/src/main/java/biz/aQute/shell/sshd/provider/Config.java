package biz.aQute.shell.sshd.provider;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface Config {
	String PID = "biz.aQute.shell.sshd";
	
	int port() default 8061;
	String address() default "localhost";

	String privateKeyPath();
}

