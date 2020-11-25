package temp;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;

public class NativeCodeTester {

	static LaunchpadBuilder builder = new LaunchpadBuilder().bndrun("bnd.bnd").runfw("org.apache.felix.framework");

	@Test
	public void test() throws Exception {
		CountDownLatch l= new CountDownLatch(10);
		
		for (int t = 0; t < 10; t++) {
			int th=t;
			Thread thread = new Thread(() -> {
				for (int times = 0; times < 10; times++) {
					try (Launchpad lp = builder.create()) {
						System.out.println(th + " " + times);
					} catch (Exception e) {
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
