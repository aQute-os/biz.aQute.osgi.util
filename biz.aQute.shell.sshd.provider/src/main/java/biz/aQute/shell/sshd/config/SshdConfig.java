package biz.aQute.shell.sshd.config;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for the Gogo SSH interface.
 */
@ObjectClassDefinition(factoryPid = SshdConfig.PID)
public @interface SshdConfig  {
	String	PID				= "biz.aQute.shell.sshd";

	/**
	 * The port to run on, by default this is {@value}
	 */
	int port() default 8062;

	/**
	 * The interface to register with. By default this is only on localhost
	 */
	String address() default "0.0.0.0";

	/**
	 * Where to store the private key. This must be in forward slashes. Default
	 * is 'host.ser' in the bundle's directory
	 */
	String hostkey() default "target/hostkey.ser";

	/**
	 * Support passwords.
	 */
	boolean passwords() default false;
	
	/**
	 * Permission for a command. The default value is {@value}, this does not allow general commands. Replace `none` with a glob expression for allowable commands  
	 */
	String permission() default "gogo.command:none";
}
