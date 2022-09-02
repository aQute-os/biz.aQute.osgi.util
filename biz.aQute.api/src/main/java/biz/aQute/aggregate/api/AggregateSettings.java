package biz.aQute.aggregate.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AggregateSettings {
	/**
	 * Override the set of promised bundles for this type. This will set the
	 * required services. This can be overridden also with a System property
	 * with the key that is the prefix
	 * {@link AggregateConstants#AGGREGATE_OVERRIDE}. If -1, the promised bundles are used.
	 */
	int override() default -1;
}
