package biz.aQute.gogo.commands.provider.test.http;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.context.ServletContextHelper;

@Component(service = ServletContextHelper.class, property = { "osgi.http.whiteboard.context.name=failingcontext",
		"osgi.http.whiteboard.context.prefix=/shouldfail" })
public class FailingContext extends ServletContextHelper {
// usually no need to implement methods 
	public FailingContext() {
		throw new RuntimeException("must fail");
	}
}