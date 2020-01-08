package biz.aQute.osgi.templates;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "biz.aQute.osgi.templates.api", name = Api.NAME, description = Api.NAME)
public @interface Api {

	public static final String NAME = "API Project Template";

	@AttributeDefinition(name = "Service Name", description = "The simple name of the primary service")
	String service() default "ExampleService";
}
