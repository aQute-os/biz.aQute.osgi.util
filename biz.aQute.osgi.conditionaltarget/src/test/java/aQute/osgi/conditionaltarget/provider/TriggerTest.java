package aQute.osgi.conditionaltarget.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.junit.Test;

public class TriggerTest {

	@Test
	public void testTrigger() throws IOException, InterruptedException {
		Semaphore s = new Semaphore(0);

		try (Trigger trigger = new Trigger(s::release, 50)) {
			Thread.sleep(100);
			assertThat(s.availablePermits()).isEqualTo(0);

			trigger.trigger();
			Thread.sleep(10);
			assertThat(s.availablePermits()).isEqualTo(0);

			Thread.sleep(100);
			assertThat(s.availablePermits()).isEqualTo(1);

			Thread.sleep(100);
			assertThat(s.availablePermits()).isEqualTo(1);

			for (int i = 0; i < 10; i++) {
				trigger.trigger();
				Thread.sleep(20);
			}
			assertThat(s.availablePermits()).isEqualTo(1);

			Thread.sleep(100);
			assertThat(s.availablePermits()).isEqualTo(2);

			trigger.trigger(); // check close
		}
		Thread.sleep(100);
		assertThat(s.availablePermits()).isEqualTo(2);
	}
}
