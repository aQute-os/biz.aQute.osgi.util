package biz.aQute.gogo.commands.provider.test.http;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletErrorPage;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

@Component(service = Servlet.class)
@HttpWhiteboardServletErrorPage(errorPage = { "myErrorPage" })
@HttpWhiteboardServletPattern("/cnf")
@HttpWhiteboardContextSelect("(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=NOT_THERE)")
public class FailingErrorPageServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

}