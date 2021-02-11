package biz.aQute.osgi.jna.support.test;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.PackageAdmin;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;

@SuppressWarnings("deprecation")
public class NativeCodeTest {

	static LaunchpadBuilder	builder	= new LaunchpadBuilder()
			.bundles("com.sun.jna,biz.aQute.osgi.jna.support.provider")
			.debug()
			.runfw("org.apache.felix.framework");

	@Service
	PackageAdmin			packageAdmin;

	@Test
	public void test() throws Exception {

		for (int times = 0; times < 3; times++) {
			int X = times;
			try (Launchpad lp = builder.create().inject(this)) {
				CountDownLatch l = new CountDownLatch(2);

				Bundle bundle = lp.bundle().header("Bundle-NativeCode", ""
						+ "    darwin/x86_64/libhello.dylib;   osname=macosx;  processor=x86_64, "
						+ "    win/x86_64/hello.dll;           osname=win32;   processor=x86_64, "
						+ "    linux/x86_64/libhello.so;       osname=linux;   processor=x86_64, "
						+ "    linux/aarch64/libhello.so;      osname=linux;   processor=aarch64, "
						+ "    linux/armv5l/libhello.so;       osname=linux;   processor=armv5l, "
						+ "    linux/armv7l/libhello.so;       osname=linux;   processor=armv7l, "
						+ "    linux/armv5l/libhello.so;       osname=linux;   processor=arm_le, "
						+ "    linux/x86/libhello.so;          osname=linux;   processor=x86")
						.header("Bundle-Activator", "biz.aQute.osgi.jna.support.test.a.HelloComponent")
						.header("-includeresource", "native, target/classes")
						.header("-conditionalpackage", "aQute.lib*").install();

				bundle.start();
				for (int t = 0; t < l.getCount(); t++) {
					int T = t;
					Thread thread = new Thread(() -> {

						for (int j = 0; j < 5; j++) {
							try {
								Thread.sleep(102);
								System.out.println(X + " " + T);
								bundle.stop();
								Thread.yield();
								bundle.start();
								Thread.yield();
								packageAdmin.refreshPackages(new Bundle[] { bundle });
							} catch (BundleException e) {
								e.printStackTrace();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						l.countDown();
					});
					thread.start();
				}
				l.await();
			}
		}
	}
}
