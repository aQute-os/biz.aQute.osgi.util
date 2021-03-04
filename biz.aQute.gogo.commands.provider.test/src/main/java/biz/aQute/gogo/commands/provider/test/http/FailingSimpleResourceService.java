package biz.aQute.gogo.commands.provider.test.http;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

@Component(service = FailingSimpleResourceService.class, property = { "osgi.http.whiteboard.resource.pattern=/static/*",
		"osgi.http.whiteboard.resource.prefix=/www" })
@HttpWhiteboardServletPattern("/cnf")
@HttpWhiteboardContextSelect("(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=NOT_THERE)")
public class FailingSimpleResourceService {
	// nothing to implement
}