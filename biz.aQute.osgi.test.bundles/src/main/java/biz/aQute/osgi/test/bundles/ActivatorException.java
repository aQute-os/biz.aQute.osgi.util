package biz.aQute.osgi.test.bundles;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


public class ActivatorException implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		throw new Exception("Oops start");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		throw new Exception("Oops stop");
	}

}
