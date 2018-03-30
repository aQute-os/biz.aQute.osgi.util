package biz.aQute.osgi.test.bundles;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


public class B implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("start b");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("stop b");
	}

}
