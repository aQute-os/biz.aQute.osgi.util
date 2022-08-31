package biz.aQute.aggregate.api;

public interface AggregateConstants {
	/**
	 * An optional header that can override the settings in the bundle that are
	 * discovered by looking at the requirements and capabilities in the
	 * osgi.service namespace. The key of the parameters is always the service
	 * type.
	 */
	String	AGGREGATE_OVERRIDE				= "Aggregate-Override";

	/**
	 * A comma separated list of actual type names. These type names must
	 * implement {@code Iterable<[serviceType]>}, where serviceType is the key
	 * of the Parameter. By default, these types are calculated by looking at
	 * the required capabilities that have a filter for an actual Type that
	 * implements Iterable.
	 */
	String	AGGREGATE_OVERRIDE_ACTUAL_ATTR	= "actual";

	/**
	 * An integer that indicates how many services of the serviceType this
	 * bundle will register at runtime. By default, these are calculated from
	 * the provided capabilities in the {@code osgi.service} namespace.
	 */
	String	AGGREGATE_OVERRIDE_PROMISE_ATTR	= "promise";

	/**
	 * Configuration prefix for a system property that overrides the total set
	 * of promised service types. The suffix must be a service type name.
	 */
	String	PREFIX_TO_OVERRIDE				= "aggregate.override.";
	/**
	 * Configuration prefix for a system property to adjust the set of promised
	 * services. The suffix must be a service type name.
	 */
	String	PREFIX_TO_ADJUST				= "aggregate.adjust.";
}
