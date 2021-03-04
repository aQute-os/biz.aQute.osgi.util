package biz.aQute.gogo.commands.provider.test.http;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import org.osgi.service.component.annotations.Component;

@Component(property = "osgi.http.whiteboard.listener=true")
public class SimpleListener implements ServletRequestListener {
	public void requestInitialized(ServletRequestEvent sre) {

	}

	public void requestDestroyed(ServletRequestEvent sre) {

	}
}