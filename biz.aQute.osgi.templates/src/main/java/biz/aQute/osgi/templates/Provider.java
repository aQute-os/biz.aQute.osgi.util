package biz.aQute.osgi.templates;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "biz.aQute.osgi.templates.provider", name = Provider.NAME, description = Provider.NAME)
public @interface Provider {

	public static final String NAME = "Provider Project Template";

	@AttributeDefinition(name = "Provider Name", description = "The simple name of the primary provider class")
	String provider() default "ExampleProviderImpl";
}
