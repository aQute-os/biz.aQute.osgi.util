package biz.aQute.gogo.commands.provider.test.http;

import org.osgi.service.component.annotations.Component;

@Component(service = SimpleResourceService.class, property = { "osgi.http.whiteboard.resource.pattern=/bar",
		"osgi.http.whiteboard.resource.prefix=/bar" })
public class SimpleResourceService {
	// nothing to implement
}