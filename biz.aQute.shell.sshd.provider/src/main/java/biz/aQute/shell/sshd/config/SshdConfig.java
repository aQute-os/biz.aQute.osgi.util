package biz.aQute.shell.sshd.config;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for the Gogo SSH interface.
 */
@ObjectClassDefinition
public @interface SshdConfig {
	String PID = "biz.aQute.shell.sshd";

	/**
	 * The port to run on, by default this is {@value}
	 */
	int port() default 8061;

	/**
	 * The interface to register with. By default this is only on localhost
	 */
	String address() default "localhost";

	/**
	 * Where to store the private key. This must be in forward slashes. Default
	 * is 'host.ser' in the bundle's directory
	 */
	String privateKeyPath() default "target/hostkey.ser";

	/**
	 * Allow access without authentication
	 */
	boolean optionalAuthentication() default false;
}
