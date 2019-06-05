package aQute.osgi.conditionaltarget.api;

import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;

/**
 * DS supports n-ary cardinality very well. It is even possible to specify a
 * minimum cardinality through configuration. However, there is no way to
 * specify a filter that crosses the members of this set. For example, if you
 * want to be enabled when an average property value over the services is higher
 * than a certain value then it requires a lot of work.
 * <p>
 * Referencing this service will make the Conditional Target Runtime (CTR) to
 * create a service that has all the property keys used in the target filter.
 * There are the following types of keys:
 * <ul>
 * <li>T – T is the target class
 * <li># – Total number of services
 * <li>#&lt;rootkey&gt; – number of values for the &lt;rootkey&gt; properties
 * that are Numbers.
 * <li>[max]&lt;rootkey&gt; – maximum numeric value of the &lt;rootkey&gt;
 * properties that are Numbers.
 * <li>[min]&lt;rootkey&gt; – minimum numeric value of the &lt;rootkey&gt;
 * properties that are Numbers.
 * <li>[sum]&lt;rootkey&gt; – sum numeric value of the &lt;rootkey&gt;
 * properties that are Numbers.
 * <li>[avg]&lt;rootkey&gt; – average numeric value of the &lt;rootkey&gt;
 * properties that are Numbers.
 * <li>Otherwise the property is the flattened list of the service properties of
 * all services in scope.
 * </ul>
 * To look for applicable services, the scope, CTR creates a service tracker on
 * the registry. If one or more T's are defined it will track those services. If
 * no T is defined, it will track any service that has property name matching a
 * rootkey in the filter.
 * <p>
 * For example:
 * 
 * <pre>
 * &#64;Reference(target = "(&([avg]bar>=4)(T=com.example.foo.Bar)")
 * ConditionalTarget&lt;Bar&gt; bars;
 * </pre>
 * 
 * @param <T>
 *            The service type the Conditional Target will be looking for. In
 *            almost all cases this is the T property in the target filter.
 */
public interface ConditionalTarget<T> extends Iterable<T>{

	/**
	 * Get the tracked services.
	 * 
	 * @return the tracked services
	 */
	List<T> getServices();

	Map<ServiceReference<T>,T> getServiceReferences();

	/**
	 * Get the current aggregate properties as calculated form the filters and
	 * the services in scope.
	 * 
	 * @return the map with the aggregate properties
	 */
	Map<String, Object> getAggregateProperties();
}
