package biz.aQute.shell.sshd.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for the Gogo SSH interface.
 */
@ObjectClassDefinition( description = "Configuration for the Gogo SSH interface.")
public @interface SshdConfig {
	String PID = "biz.aQute.shell.sshd";

	/**
	 * The port to run on, by default this is {@value}
	 */
	@AttributeDefinition(description = "The port to run on")
	int port() default 8062;

	/**
	 * The interface to register with. By default this is only on 0.0.0.0
	 */
	@AttributeDefinition(description = "The interface to register with.")
	String address() default "0.0.0.0";

	/**
	 * Where to store the private key. This must be in forward slashes. Default is
	 * 'target/host.ser' in the bundle's directory
	 */
	@AttributeDefinition(description = "Where to store the private key. This must be in forward slashes.")
	String hostkey() default "target/hostkey.ser";

	/**
	 * Support passwords.
	 */
	@AttributeDefinition(description = "Support passwords.")
	boolean passwords() default false;

	/**
	 * Permission for a command. The default value is {@value}, this does not allow
	 * general commands. Replace `none` with a glob expression for allowable
	 * commands
	 */
	@AttributeDefinition(description = "Permission for a command. The default value is gogo.command:none, this does not allow general commands. Replace `none` with a glob expression for allowable commands")
	String permission() default "gogo.command:none";
}
