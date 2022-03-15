package biz.aQute.osgi.logger.forwarder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogForwarderTest {

	@Test
	public void testSimple() {
		Logger l = LoggerFactory.getLogger(LogForwarderTest.class);

		l.debug("Hello world");
		assertThat(LogForwarder.SINGLETON.queue).isNotEmpty();

		LogService mock = Mockito.mock(LogService.class);
		org.osgi.service.log.Logger mlogger = Mockito.mock(org.osgi.service.log.Logger.class);
		Mockito.when(mock.getLogger(LogForwarderTest.class.getName()))
			.thenReturn(mlogger);
		Mockito.when(mock.getLogger(LogForwarder.class.getName()))
			.thenReturn(mlogger);
		LogForwarder.addLogService(mock, 1);
		assertThat(LogForwarder.SINGLETON.queue).isEmpty();
	}

	@Test
	public void testFormatter() {
		Set<Object> visited = new HashSet<>();
		String s = Facade.print(new Object[] {
			new int[] {
				1, 2, 3, 4
			}, new float[] {
				3.14f, 4567890
			}, new String[] {
				"a", "b"
			}
		}, visited).toString();
		assertThat(s).isEqualTo("[[1,2,3,4],[3.14,4567890.0],[a,b]]");
	}

	@Test
	public void testFormatterWithObjectThatThrowsNPEOnHashCode() {
		class Idiot {
			@Override
			public int hashCode() {
				throw new NullPointerException();
			}

			@Override
			public boolean equals(Object o) {
				return this == o;
			}

			@Override
			public String toString() {
				return "42";
			}
		}
		String s = Facade.print(new Object[] {
			new Idiot()
		}, null)
			.toString();
		assertThat(s).isEqualTo("[42]");
	}

	@Test
	public void testFormatterCycles() {
		Set<Object> visited = new HashSet<>();
		Object[] array = new Object[10];
		array[0] = array;
		array[1] = new int[] {
			1, 2, 3, 4
		};
		array[2] = new float[] {
			3.14f, 4567890
		};
		array[3] = new Object[] {
			array
		};

		String s = Facade.print(array, visited)
			.toString();
		assertThat(s).contains("cycle : ");
	}

	@Test
	public void testFormatterSize() {
		Set<Object> visited = new HashSet<>();
		Object[] array = new Object[10];
		int[] ints = new int[1000];
		for (int i = 0; i < 1000; i++)
			ints[i] = -i;
		array[5] = ints;
		String s = Facade.print(array, visited)
			.toString();
		assertThat(s).endsWith(" ...")
			.hasSizeLessThan(1040);
	}

}
