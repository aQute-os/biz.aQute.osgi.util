package biz.aQute.gogo.commands.provider;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ExampleProviderImplTest {
	@Test
	public void simple() {
		GogoCommands impl = new GogoCommands();
		assertNotNull(impl);
	}

}
