package biz.aQute.shell.sshd.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for the Gogo SSH interface in an insecure mode.
 */
@ObjectClassDefinition(description = "Configuration for the Gogo SSH interface in an insecure mode.")
public @interface SshdConfigInsecure {
	String PID = "biz.aQute.shell.sshd.insecure";

	/**
	 * The port to run on, by default this is {@value}
	 */
	@AttributeDefinition(description = "The port to run on")
	int port() default 8061;

	/**
	 * Where to store the private key. This must be in forward slashes. Default is
	 * 'target/host.ser' in the bundle's directory
	 */
	@AttributeDefinition(description = "Where to store the private key. This must be in forward slashes.")
	String hostkey() default "target/host.ser";
	
	/**
	 * The only accepted user name
	 */
	@AttributeDefinition(description = "The only accepted user name")
	String user() default "brave";
	
	/**
	 * The only accepted user name. THIS IS NOT SECURE!
	 */
	@AttributeDefinition(type = AttributeType.PASSWORD,description = "The only accepted password")
	String password() default "insecure";
}
