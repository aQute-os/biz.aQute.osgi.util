package biz.aQute.gogo.commands.provider.test.http;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.osgi.service.component.annotations.Component;

@Component(service = Servlet.class)
@HttpWhiteboardServletPattern("/foo")
public class SimpleServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

}