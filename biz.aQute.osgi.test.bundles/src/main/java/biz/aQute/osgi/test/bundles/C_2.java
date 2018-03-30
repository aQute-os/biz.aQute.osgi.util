package biz.aQute.osgi.test.bundles;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


public class C_2 implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("start c_2");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("stop c_2");
	}

}
