package biz.aQute.shell.sshd.config;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for the Gogo SSH interface in an insecure mode.
 */
@ObjectClassDefinition
public @interface SshdConfigInsecure {
	String	PID	= "biz.aQute.shell.sshd.insecure";

	/**
	 * The port to run on, by default this is {@value}
	 */
	int port() default 8061;

	/**
	 * Where to store the private key. This must be in forward slashes. Default
	 * is 'host.ser' in the bundle's directory
	 */
	String hostkey() default "target/host.ser";
}
