package biz.aQute.cryptonicom;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import biz.aQute.cryptonicom.ExampleProviderImpl;

public class ExampleProviderImplTest {
	@Test
	public void simple() {
		ExampleProviderImpl impl = new ExampleProviderImpl();
		assertNotNull(impl);
	}

}
