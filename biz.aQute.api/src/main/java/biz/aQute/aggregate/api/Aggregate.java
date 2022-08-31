package biz.aQute.aggregate.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Aggregate {
	/**
	 * Adjust the set of promised services for this type. For example, if 4
	 * services of the associated service type are promised by the bundles then
	 * making the adjust -2 will reduce the number of discovered services to
	 * 4-2=2.
	 * <p>
	 * This can be overridden with a system property with the key that is the
	 * prefix {@link AggregateConstants#AGGREGATE_ADJUST}
	 * <p>
	 * Adjustments are ignored if any override is used.
	 */
	int adjust() default 0;

	/**
	 * Override the set of promised services for this type. This will set the
	 * required services. This can only be overridden with a System property
	 * with the key that is the prefix
	 * {@link AggregateConstants#AGGREGATE_OVERRIDE}. If -1, the promised with
	 * adjustments are used.
	 */
	int override() default -1;
}
