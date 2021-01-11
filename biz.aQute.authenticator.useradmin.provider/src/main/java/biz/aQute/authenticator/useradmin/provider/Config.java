package biz.aQute.authenticator.useradmin.provider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface Config {
	public enum Algorithm {
		PBKDF2WithHmacSHA1
	}

	byte[] salt() default { 0x2f, 0x68, (byte) 0xcb, 0x75, 0x6c, (byte) 0xf1, 0x74, (byte) 0x84, 0x2a, (byte) 0xef };

	int iterations() default 997;

	Algorithm algorithm() default Algorithm.PBKDF2WithHmacSHA1;

	@AttributeDefinition(type = AttributeType.PASSWORD, description = "If set, the hashed password of user must equal this value.")
	String _root() default "";
}
