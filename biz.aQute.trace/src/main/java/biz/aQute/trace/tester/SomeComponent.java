package biz.aQute.trace.tester;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component
public class SomeComponent {

	@Activate
	void activate(BundleContext c) {
		System.out.println(">>>");
	}

	@Deactivate
	void deactivate() {
		System.out.println("<<<");
	}
}
