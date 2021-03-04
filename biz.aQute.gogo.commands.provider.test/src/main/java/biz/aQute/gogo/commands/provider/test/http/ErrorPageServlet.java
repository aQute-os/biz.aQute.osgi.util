package biz.aQute.gogo.commands.provider.test.http;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletErrorPage;

@Component(service = Servlet.class)
@HttpWhiteboardServletErrorPage(errorPage = { "myErrorPage" })
public class ErrorPageServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

}