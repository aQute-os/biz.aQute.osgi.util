package biz.aQute.foreign.python.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Requirement;
import org.osgi.namespace.extender.ExtenderNamespace;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.TYPE, ElementType.PACKAGE
})
@Requirement(namespace = ExtenderNamespace.EXTENDER_NAMESPACE, name = ForeignPythonConstants.EXTENDER_NAME, version=ForeignPythonConstants.VERSION)
public @interface RequireForeignPython {
}
