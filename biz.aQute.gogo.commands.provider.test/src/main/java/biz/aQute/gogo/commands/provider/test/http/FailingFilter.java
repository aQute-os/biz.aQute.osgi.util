package biz.aQute.gogo.commands.provider.test.http;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;

@HttpWhiteboardContextSelect("(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=NOT_THERE)")
@Component(property = "osgi.http.whiteboard.filter.pattern=/*", scope = ServiceScope.PROTOTYPE)
public class FailingFilter implements javax.servlet.Filter {

	public void init(javax.servlet.FilterConfig cfg) {
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) {

		try {
			chain.doFilter(req, resp);
		} catch (IOException | ServletException e) {
			e.printStackTrace();
		}
	}
}