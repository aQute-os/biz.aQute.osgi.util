package biz.aQute.gogo.commands.provider.test.http;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(property = "osgi.http.whiteboard.filter.pattern=/*", scope = ServiceScope.PROTOTYPE)
public class SimpleFilter implements javax.servlet.Filter {

	public void init(javax.servlet.FilterConfig cfg) {

	}

	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) {
		// do something before the servlet
		try {
			chain.doFilter(req, resp);
		} catch (IOException | ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// do something after the servlet
	}
}