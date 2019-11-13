package biz.aQute.aspects.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WeavingHook;

public class Activator implements BundleActivator {

	private WeaverImpl weaver;

	@Override
	public void start(BundleContext context) throws Exception {

		this.weaver = new WeaverImpl(context);
		context.registerService(WeavingHook.class, this.weaver, null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {}

}
