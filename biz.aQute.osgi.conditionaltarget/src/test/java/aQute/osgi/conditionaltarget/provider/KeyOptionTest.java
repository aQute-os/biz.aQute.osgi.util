package aQute.osgi.conditionaltarget.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class KeyOptionTest {

	@Test
	public void testKeys() {
		List<Number> l = Arrays.asList(1,2,3,4,1,1);

		assertThat(KeyOption.AVG.value(l)).isEqualTo(2.0D);
		assertThat(KeyOption.COUNT.value(l)).isEqualTo(6);
		assertThat(KeyOption.MAX.value(l)).isEqualTo(4.0D);
		assertThat(KeyOption.MIN.value(l)).isEqualTo(1.0D);
		assertThat(KeyOption.SUM.value(l)).isEqualTo(12.0D);
		assertThat(KeyOption.UNQ.value(l)).isEqualTo(4L);
	}
}
