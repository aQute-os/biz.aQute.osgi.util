package biz.aQute.osgi.jna.support.test.a;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sun.jna.Native;

import biz.aQute.osgi.jna.support.provider.DynamicLibrary;

public class HelloComponent implements BundleActivator {
	DynamicLibrary<Hello> hello;

	public void simple() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
			Native.setProtected(true);
			System.out.println("start");
			System.setProperty("soft", false+"");
			hello = new DynamicLibrary<>("hello", Hello.class, "-foobar");
			hello.get().ifPresent( Hello::hello);
			hello.get().ifPresent( (h)-> {
				Foo foo = h.create();
				System.out.println( "Before " + foo + " " + foo.text);
				h.fill(foo);
				System.out.println( "Middle " + foo + " " + foo.text);
				foo.setAutoRead(false);
				h.close(foo);
				System.out.println("After " + foo + " " + foo.text);
				
				Foo foo2 = new Foo();
				System.out.println( "New " + foo2 + " " + foo2.text);
				h.fill(foo2);
				System.out.println( "Filled " + foo2 + " " + foo2.text);
				
			});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("bye");
		hello.close();
	}

}