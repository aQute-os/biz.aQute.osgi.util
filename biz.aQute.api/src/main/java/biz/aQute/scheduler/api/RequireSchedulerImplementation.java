package biz.aQute.scheduler.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.implementation.ImplementationNamespace;

/**
 * Require an implementation for the this specification
 */

@org.osgi.annotation.bundle.Requirement(
	    effective = "active",
	    namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE,
	    name = Constants.SPECIFICATION_NAME,
	    version = Constants.SPECIFICATION_VERSION
	)
@Retention(RetentionPolicy.CLASS)
public @interface RequireSchedulerImplementation {}