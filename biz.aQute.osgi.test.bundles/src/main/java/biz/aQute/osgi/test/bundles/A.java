package biz.aQute.osgi.test.bundles;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


public class A implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("start a");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("stop a");
	}

}
