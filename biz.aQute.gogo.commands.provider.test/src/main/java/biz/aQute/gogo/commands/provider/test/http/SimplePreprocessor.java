package biz.aQute.gogo.commands.provider.test.http;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.Preprocessor;

@Component(service = Preprocessor.class)
public class SimplePreprocessor implements Preprocessor {

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		chain.doFilter(request, response);
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		// initialize the preprocessor
	}

	public void destroy() {
		// clean up
	}
}