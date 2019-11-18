package biz.aQute.trace.tester;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component
public class LazyComponent implements Foo {

	@Activate
	void activate() {
		System.out.println("Act Lazy");
	}

	@Deactivate
	void deactivate() {
		System.out.println("Deact lazy");
	}
}
