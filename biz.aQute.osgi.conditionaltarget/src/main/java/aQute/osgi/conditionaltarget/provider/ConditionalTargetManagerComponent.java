package aQute.osgi.conditionaltarget.provider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;

/**
 * The component that facades the actual worker
 */
@Component(immediate = true)
public class ConditionalTargetManagerComponent {

	@Reference
	ServiceComponentRuntime						scr;

	private ConditionalTargetManager		handler;
	private ServiceRegistration<ListenerHook>	registerService;

	@Activate
	void activate(BundleContext context) {
		this.handler = new ConditionalTargetManager(context, scr);
		this.registerService = context.registerService(ListenerHook.class, this.handler, null);
	}

	@Deactivate
	synchronized void deactivate() {
		this.registerService.unregister();
		this.handler.close();
	}
}
