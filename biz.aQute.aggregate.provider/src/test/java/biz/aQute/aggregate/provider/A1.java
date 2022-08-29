package biz.aQute.aggregate.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component
public class A1 implements IF {

	@Activate
	public A1() {
		System.out.println("active");
	}
}
