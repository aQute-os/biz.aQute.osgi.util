@Requirement(namespace=ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name=AggregateConstants.IMPLEMENTATION_NAME, version=AggregateConstants.API_VERSION)
@org.osgi.annotation.bundle.Export
@Version(biz.aQute.aggregate.api.AggregateConstants.API_VERSION)
package biz.aQute.aggregate.api;

import org.osgi.annotation.bundle.Requirement;
import org.osgi.annotation.versioning.Version;
import org.osgi.namespace.implementation.ImplementationNamespace;

/**
 * An API to do thread based stuff
 */
