package temp;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class NativeCodeTest implements BundleActivator {

	public void simple() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		try {
			System.setProperty("soft", false+"");
			try (DynamicLibrary<Hello> hello = new DynamicLibrary<>("hello", Hello.class, "-foobar")) {
				hello.get().ifPresent( Hello::hello);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		System.out.println("bye");
	}

}