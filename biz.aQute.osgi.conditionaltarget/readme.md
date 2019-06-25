# Conditional Target

The whiteboard pattern is quite popular in OSGi and for good reasons. However, the pattern is not without its
drawbacks. Sometimes you, mostly at startup, you want a certain set of services to populate your whiteboard
list. For example, assume you have persistent storage and you need at least 3 remote servers in 2 different
regions. 

This is hard to model with services because you can only select on a single service. OSGi has no way to define
a filter over multiple services. The Conditional Target provides this service. It is a generic service where
the type parameter is the service type, generally the whiteboard type, you're interested in. 

If you're using Declarative Services (DS) and your whiteboard service was `Foo`, then it would look like:

    @Reference
    ConditionalTarget<Foo>       strings;

The Conditional Target provider detects that you're looking for Foo services and it will create a Conditional Target
service that provides access to all listed Foo services visible to you. It also provides access to the 
_aggregate_ properties. This is the API:

	public interface ConditionalTarget<T> extends Iterable<T>{
	
		/**
		 * Get the tracked services.
		 * 
		 * @return the tracked services
		 */
		List<T> getServices();

        /**
         * Get all service references
         */
		Map<ServiceReference<T>,T> getServiceReferences();
	
		/**
		 * Get the current aggregate properties as calculated form the filters and
		 * the services in scope.
		 * 
		 * @return the map with the aggregate properties
		 */
		Map<String, Object> getAggregateProperties();
	}

## Aggregate Properties

The aggregate properties are the _aggregate_ of the Foo service properties. For example, a Foo⁰, Foo¹, and Foo² 
are registered.  Each service has a `bar` property that is set to 0, 1, 2 respectively. The aggregate property `bar` is
then a collection of [0,1,2]. Arrays and collections are merged except for `byte[]`.

Since the Conditional Target service has the aggregate properties of all Foo services, we can now select across
the services using a standard filter. For example, we want to have at least `bar=0 and bar=1` registered or 
`bar=1 and bar=2`. 

## Target Filter

With DS we can make a reference conditional with the `target` annotation method. This is a filter.

    @Reference(target="(|(&((bar=0)(bar=1)))(&((bar=1)(bar=0))))")
    ConditionalTarget<Foo>      foos;

## Calculated values

Besides calculatiing the aggregate values from the properties of the whiteboard services, the Conditional Target also 
accepts filters that have _calculated_ fields. For example, if you need at least three services that have a `bar`
property then you can filter on `#bar`. There are a number of calculated values:

* `#key`     – Calculates the number of `key` properties
* `[avg]key` – Calculates the average of all `key` properties
* `[min]key` – Calculates the minimum of all `key` properties
* `[max]key` – Calculates the maximum of all `key` properties
* `[sum]key` – Calculates the sum of  all `key` properties
* `[unq]key` – Calculates the number of unique `key` properties

    @Reference(target="([sum]bar>=3)")
    ConditionalTarget<Foo>      foos;

## Configuration

This would be interesting but what really makes it very useful is that DS allows the `target` to be overridden
by configuration. If the configuraiton of this object contains a field `foos.target` (the name of the field followed
by the word `.target`) then the value is taken as the filter. 

## Use Case

Assume we have a service R that represents a link to a remote service. For performance and reliability reasons we 
require at least 3 of those services to be present before we can start. Additionally, these services must come from
at least 2 different regions.

For this reason, we define a property `region` that can take the values `south`, `east`, `north`, and `west`. 

The reference to the Conditional Target can now be defined as:

    @Reference(target="(&(#>=3)([unq]region>=2))")
    ConditionalTarget<R>      foos;

## How it Works

The Conditional Target provider uses the framework Hooks to detect the interest in Conditional Target services. It
then uses the Service Component Runtime service to find the Component Configuration DTO that is looking for the
Conditional Target. It uses reflection to find the generic type.

It will then:

* Register a Conditional Target, and
* Track services of the generic type, and
* Keep the aggregate properties of the Conditional Target updated.

The services are checked out from the registry via the Bundle Context of the requesting bundle.

