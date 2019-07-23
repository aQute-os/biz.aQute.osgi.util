package aQute.osgi.conditionaltarget.provider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;

import aQute.osgi.conditionaltarget.api.ConditionalTarget;

/**
 * The component that facades the actual worker
 */
@Component(immediate = true)
public class CTServerComponent {

	@Component(enabled=false)
	public abstract class ConditionalTargetDummy implements ConditionalTarget<Object>{
	}

	@Reference
	ServiceComponentRuntime						scr;

	private CTServer		server;
	private ServiceRegistration<ListenerHook>	registerService;

	@Activate
	void activate(BundleContext context) {
		this.server = new CTServer(context, scr);
		this.registerService = context.registerService(ListenerHook.class, this.server, null);
	}

	@Deactivate
	synchronized void deactivate() {
		this.registerService.unregister();
		this.server.close();
	}
}
