package biz.aQute.authorization.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.annotation.bundle.Requirement;
import org.osgi.namespace.implementation.ImplementationNamespace;

/**
 * Require an implementation for the this specification
 */
@Requirement(namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, filter = "(&("
	+ ImplementationNamespace.IMPLEMENTATION_NAMESPACE + "=" + AuthorizationConstants.AUTHORIZATION_SPECIFICATION_NAME
	+ ")${frange;${version;==;" + AuthorizationConstants.AUTHORIZATION_SPECIFICATION_VERSION + "}})")
@Retention(RetentionPolicy.CLASS)
public @interface RequireAuthorizationImplementation {}
