package biz.aQute.osgi.logger.forwarder;

import static org.assertj.core.api.Assertions.assertThat;

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
		Mockito.when(mock.getLogger(LogForwarderTest.class.getName())).thenReturn(mlogger);
		LogForwarder.addLogService(mock, 1);
		assertThat(LogForwarder.SINGLETON.queue).isEmpty();
	}
}
