package biz.aQute.aws.config;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface SESConfig {
	String region() default "https://email.us-east-1.amazonaws.com/";

	String from();

	String id();

	String _secret();

}
