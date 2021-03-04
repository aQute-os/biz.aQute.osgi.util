package biz.aQute.gogo.commands.provider.test.http;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;

@HttpWhiteboardContextSelect("(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=NOT_THERE)")
@Component(property = "osgi.http.whiteboard.listener=true")
public class FailingListener implements ServletRequestListener {
	public void requestInitialized(ServletRequestEvent sre) {

	}

	public void requestDestroyed(ServletRequestEvent sre) {

	}
}