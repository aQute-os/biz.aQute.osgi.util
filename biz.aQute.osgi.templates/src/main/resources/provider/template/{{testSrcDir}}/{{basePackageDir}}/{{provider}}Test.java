package {{basePackageName}};

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import {{basePackageName}}.{{provider}};

public class {{provider}}Test {
	@Test
	public void simple() {
		{{provider}} impl = new {{provider}}();
		assertNotNull(impl);
	}

}
