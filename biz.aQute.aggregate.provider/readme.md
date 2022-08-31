
# Aggregate State Revisited

Once of the few really complex problems in OSGi is to prevent the so called 'jojoing' during the initialization.
A service gets registered but then later registrations require this service to pick up these changes and
it becomes unregistered. In this case dynamic references often do not work because in many cases
it is necessary to pick up _all_ the references that are installed.

## TL;DR

Provides a solution to pickup the number of services that are promised by the installed bundles.
This is a template for using this aggregate state:

    @Component
    public class Server {
     
        public interface Plugin {}
   
        public interface PluginAggregate extends Iterable<Plugin> {}
        
        @Activate
        public Server( @Reference PluginAggregate plugins ) {
            for (Plugin p : plugins) {
               ...
            }
        }

## Other Solutions

There are a number of solutions to minimize this problem. 

* Startlevels – If you can control start levels you can make sure that the server bundle is started
  later than its plugins. However, this requires a global overview and sometimes the server bundle
  also registers some handlers.
* Declarative Services Conditions – Conditions provide a gate before a component is activated. This is
  name based. To a certain extent this aggregate state solution is similar, the service that it 
  registers also acts as a gate. However, it is type safe and the service used as the gate also
  provides access to the constituents.
  
## Design

The basic design is to register a special service that gates the registration of the handler/plugin services.
This gating service is the _actual type_ and the handler/plugin services are the service types.

The actual type should not be registered before a _sufficient_ number of service types are registered.

There are two parts to this problem. First, how do we find what is 'sufficient' for a service Type and
how do we find the actual type? To find out the number of installed services as well as the actual type, this bundle scans
all ACTIVE bundles at startup & when the bundles change. 

The service types are found by looking for `osgi.service` capabilities. The `objectClass` attribute in this
capability is counted as 1 registered service type. This is not always true but can be overridden in 
numerous ways. This gives us a number of 'promised' services.

    Provide-Capability osgi.service;objectClass:List<String>="Plugin"

The actual type is slightly more complicated. A component that wants to be gated by all installed
services started, can create a new interface that extends `Iterable<S>`, where `S` is a service
type. This bundle scans the bundles and finds any requirement for an actual type.

    public interface PluginAggregate extends Iterable<Plugin> {}

The fact that this `PluginAggregate` service is used in a `@Reference` will force an `osgi.service` requirement. 
This requirement is recorded, assuming bnd, in the manifest in the `Require-Capability` header.

    Require-Capability  osgi.service;filter:="(objectClass=PluginAggregate)"

When this bundle analyzes this header, it checks if the object class extends `Iterable`.
Although Java erases much of the generics, it maintains the generic information for extended interfaces. 
This enables the bundle to find that the  constituent class is `Plugin`, called the _service type_. 
       
Therefore, after analyzing the bundle we can establish the following information for each service type:

* the service type
* a list of actual types
* promised services
* discovered services (registered)

When it is found that a service type has more or equal services registered it can create proxies for the
its actual types and register these as  OSGi services. This works as a gate for the components
that uses an actual type as a reference. Since at that time, all promised services are registered,
the component will also be able to depend on all promised services. The actual type service is an
Iterable and can be used to iterate over the active set of service types.

When the discovered services change, or the set of ACTIVE bundles changes, then the situation is updated
which might result in the unregistration of the gating actual type service.

This is the basic theory. The actual implementation also needs to handle that multiple bundles
can use the same actual type.

## Configuration

There are a number of ways to override the number of promised services.

### System properties 

Both the service type and the actual type can _override_ and _adjust_ the required services
that should be discovered before the actual type service is registered. This is a bit overdone
and should preferably not be used but it allows the control of the system to the finest detail,
something that is sometimes required in the field.

Adjusting means that the promised services gets added, but can be negative, to the promised
services. To adjust for a service type, a System property can be set that starts with `aggregate.adjust.`
and is suffixed with a type name. For example, the following will add +3 to the promised 
services for all actual types.

    System.setProperty( AggregateConstants.PREFIX_TO_ADJUST + "Plugin", "+3");

If a property is set with the actual type name, it will adjust if only for that actual type.
    
    System.setProperty( AggregateConstants.PREFIX_TO_ADJUST + "PluginAggregate", "+3");
    
In some cases an override is needed, i.e. the promised services should be discarded. This can be
done with the System properties that start with `aggregate.override.` and are suffixed with
a type name.

    System.setProperty( AggregateConstants.PREFIX_TO_OVERRIDE + "Plugin", "3");

If a property is set with the actual type name, it will override if only for that actual type.
    
    System.setProperty( AggregateConstants.PREFIX_TO_ADJUST + "PluginAggregate", "3");
    
### @Aggregate annotation

The Aggregate annotation is applied to an actual type and will therefore be able to set the 
local adjust and overrides. System properties override the annotation values for override and  adjust.

## Priority

When the required services are calculated to see if the actual type can be registered, the following order is used.

* actual type local override if >= 0
* service type override if > =0
* promised + adjust + localAdjust


