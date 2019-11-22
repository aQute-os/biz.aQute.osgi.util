package biz.aQute.trace.tester;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component
public class SomeComponent4 {

	@Activate
	void activate(BundleContext c) {
		System.out.println(">>>");
		ServiceReference<Foo> ref = c.getServiceReference(Foo.class);
		Foo service = c.getService(ref);
	}

	@Deactivate
	void deactivate() {
		System.out.println("<<<");
	}
}
