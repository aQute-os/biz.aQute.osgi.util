package biz.aQute.trace.runpath.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WeavingHook;

import biz.aQute.trace.activate.ActivationTracer;

public class Activator implements BundleActivator {

	final public static boolean	IMMEDIATE	= true;

	/**
	 * You can define a framework/system property {@value #EXTRA}. This can be a
	 * string with the following format:
	 *
	 * <pre>
	 * 			EXTRA 	::= spec ( ',' spec )
	 * 			spec	::= class ':' method ':' action
	 * </pre>
	 */
	public static String		EXTRA		= "aQute.trace.extra";
	public static String		DEBUG		= "aQute.trace.debug";

	private WeaverImpl			weaver;

	@Override
	public void start(BundleContext context) throws Exception {

		ActivationTracer.debug = context.getProperty(DEBUG) != null;
		this.weaver = new WeaverImpl(context);

		doExtra(context);

		context.registerService(WeavingHook.class, this.weaver, null);

		context.addServiceListener(e -> {
		});
		context.addBundleListener(e -> {});
		context.addFrameworkListener(e -> {});
	}

	private void doExtra(BundleContext context) {
		String extra = context.getProperty(EXTRA);
		if (extra != null) {
			String specs[] = extra.split("\\s*,\\s*");
			for (String spec : specs) {
				ActivationTracer.trace(spec);
			}
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		ActivationTracer.close();
		this.weaver.close();
	}

}
