package biz.aQute.trace.tester.constructor;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(immediate = true, enabled = false)
public class ConstructorComponent implements Foo {

	@Activate
	public ConstructorComponent(BundleContext bc) {
		System.out.println("ConstructorComponent");
	}

	@Deactivate
	void deactivate() {
		System.out.println("Deact");
	}
}
