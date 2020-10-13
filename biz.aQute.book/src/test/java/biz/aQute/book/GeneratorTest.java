package biz.aQute.book;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import aQute.lib.env.Env;

public class GeneratorTest {

	@Test
	public void testSimple() throws Exception {
		Env env = new Env();
		env.setBase(env.getFile("test/book"));
		env.setProperties(env.getFile("book.bnd"));

		Generator g = new Generator(env);
		assertThat(g.getErrors()).isEmpty();
		System.out.println(g.generate());
	}

	@Test
	public void testBook() throws Exception {
		Env env = new Env();
		env.setBase(env.getFile("~/Desktop/Dropbox/alloy-book"));
		env.setProperties(env.getFile("book.bnd"));

		Generator g = new Generator(env);
		assertThat(g.isOk()).isTrue();
		System.out.println(g.generate());
	}
}
