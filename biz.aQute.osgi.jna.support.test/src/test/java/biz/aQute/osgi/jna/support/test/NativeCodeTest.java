package biz.aQute.osgi.jna.support.test;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;

public class NativeCodeTest {

	static LaunchpadBuilder builder = new LaunchpadBuilder().bundles("com.sun.jna;version=5,biz.aQute.osgi.jna.support.test.a, biz.aQute.osgi.jna.support.provider")
			.runfw("org.apache.felix.framework");

	@Test
	public void test() throws Exception {
		
		for (int times = 0; times < 3; times++) {
			int X = times;
			try (Launchpad lp = builder.create()) {
				CountDownLatch l= new CountDownLatch(5);
				Bundle bundle = lp.getBundle("biz.aQute.osgi.jna.support.test.a").get();

				for (int t = 0; t < l.getCount(); t++) {
					int T = t;
					Thread thread = new Thread(() -> {
						
						for (int j=0; j<10; j++) {
							try {
								Thread.sleep(102);
								System.out.println(X + " " + T);
								bundle.stop();
								Thread.yield();
								bundle.start();
								Thread.yield();
								bundle.update();
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
