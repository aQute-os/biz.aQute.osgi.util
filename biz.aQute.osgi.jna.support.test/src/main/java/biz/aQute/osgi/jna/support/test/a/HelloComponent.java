package biz.aQute.osgi.jna.support.test.a;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import biz.aQute.osgi.jna.support.provider.DynamicLibrary;

public class HelloComponent implements BundleActivator {
	DynamicLibrary<Hello> hello;

	public void simple() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
			System.out.println("start");
			System.setProperty("soft", false+"");
			hello = new DynamicLibrary<>("hello", Hello.class, "-foobar");
			hello.get().ifPresent( Hello::hello);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("bye");
		hello.close();
	}

}