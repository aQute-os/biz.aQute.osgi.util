package biz.aQute.aggregate.api;

import org.osgi.annotation.bundle.Capability;
import org.osgi.namespace.implementation.ImplementationNamespace;

@Capability(namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name = AggregateConstants.IMPLEMENTATION_NAME, version = AggregateConstants.API_VERSION)
public @interface AggregateImplementation {
}
