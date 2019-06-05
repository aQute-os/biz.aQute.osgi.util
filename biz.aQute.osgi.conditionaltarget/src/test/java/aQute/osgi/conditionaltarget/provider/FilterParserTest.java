package aQute.osgi.conditionaltarget.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import aQute.osgi.conditionaltarget.provider.FilterParser;

public class FilterParserTest {

	@Test
	public void testSimple() {
		assertThat(FilterParser.getAttributes("(&(a=*)(b=*)(|(c=*)(d=*)(!(e=*))(&(f=*))))")).containsExactlyInAnyOrder("a","b", "c", "d", "e", "f");
		assertThat(FilterParser.getAttributes("(&(a=*)(b=*))")).containsExactlyInAnyOrder("a","b");
		assertThat(FilterParser.getAttributes("(a=*)")).containsExactly("a");
	}


	@Test
	public void testEscape() {
		assertThat(FilterParser.getAttributes("(a=fff\\()")).containsExactly("a");
		assertThat(FilterParser.getAttributes("(a=fff\\)\\\\)")).containsExactly("a");
		assertThat(FilterParser.getAttributes("(a=fff\\))")).containsExactly("a");
	}

	@Test
	public void testOurFunnyNames() {
		assertThat(FilterParser.getAttributes("(&(a=*)(#a=*)([uniq]a=*))")).containsExactlyInAnyOrder("a", "#a", "[uniq]a");
	}
}
