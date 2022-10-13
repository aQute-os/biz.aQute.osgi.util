package biz.aQute.osgi.service.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Capability;

/**
 * Add the marked type as a service capability  
 *
 */
@Capability(namespace = "osgi.service", effective = "active", attribute = "objectClass=${@class}")
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ObjectClass {}