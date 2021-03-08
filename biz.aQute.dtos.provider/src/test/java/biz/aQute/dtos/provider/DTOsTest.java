package biz.aQute.dtos.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.osgi.dto.DTO;

import biz.aQute.dtos.api.DTOs.Difference;
import biz.aQute.dtos.api.TypeReference;

public class DTOsTest {

	final DTOsProvider 	dtos = new DTOsProvider();
	
	
	
	@Test
	public void testSimpleConversion() throws Exception {

		assertEquals(100D, dtos.convert("100")
			.to(double.class), 0.1D);
		assertEquals(10D, dtos.convert(10f)
			.to(double.class), 0.1D);
		assertEquals(100D, dtos.convert(100L)
			.to(double.class), 0.1D);

		assertEquals(Arrays.asList(100F), dtos.convert(100L)
			.to(new TypeReference<List<Float>>() {}));

		long[] expected = new long[] {
			0x40L, 0x41L, 0x42L
		};
		byte[] source = "@AB".getBytes();
		long[] result = dtos.convert(source)
			.to(long[].class);

		assertTrue(Arrays.equals(expected, result));
	}

	
	
	
	
	
	
	
	
	
	
	
	public static class A extends DTO {
		public int a;
		public String s;
		public List<B>	bs = new ArrayList<>();
		public List<A> parents = new ArrayList<>();
	}
	public static class B extends DTO {
		public short c;
	}
	
	@Test
	public void testSimple() throws Exception {
		A a = new A();
		a.a= 1;
		a.s="string";
		
		B b = new B();
		b.c = 10;
		a.bs.add( b);
		A x = new A();
		x.a=10;
		a.parents.add(x);
		
		A aa = dtos.deepCopy(a);
		
		assertThat(aa.a).isEqualTo(1);
		assertThat(aa.s).isEqualTo("string");
		assertThat(aa.bs).hasSize(1);
		assertThat(dtos.deepEquals(aa, a)).isTrue();
		
		assertThat(dtos.get(a, "s").unwrap()).isEqualTo("string");
		assertThat(dtos.get(a, "bs.0.c").unwrap()).isEqualTo((short)10);
		
	}

	/*
	 * Show Map -> Interface
	 */
	enum Option {
		bar,
		don,
		zun
	}

	interface FooMap {
		short port();

		String host();

		Set<Option> options();
	}

	@Test
	public void testInterfaceAsMap() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("port", 10);
		map.put("host", "localhost");
		map.put("options", Arrays.asList("bar", "don", "zun"));

		FooMap foomap = dtos.convert(map)
			.to(FooMap.class);

		assertEquals((short) 10, foomap.port());
		assertEquals("localhost", foomap.host());
		assertEquals(EnumSet.allOf(Option.class), foomap.options());
	}

	/*
	 * Show DTO to map
	 */

	public static class MyData extends DTO {
		public short	port;
		public String	host;
		public Option[]	options;
	}

	@Test
	public void testDtoAsMap() throws Exception {
		MyData m = new MyData();
		m.port = 20;
		m.host = "example.com";
		m.options = new Option[] {
			Option.bar, Option.don, Option.zun
		};

		Map<String, Object> map = dtos.asMap(m);

		assertEquals(Arrays.asList("host", "options", "port"), new ArrayList<String>(map.keySet()));
		assertEquals((short) 20, map.get("port"));
		assertEquals("example.com", map.get("host"));
	}

	/*
	 * Show JSON
	 */

	@Test
	public void testJSON() throws Exception {
		MyData m = new MyData();
		m.port = 20;
		m.host = "example.com";
		m.options = new Option[] {
			Option.bar, Option.don, Option.zun
		};

		String json = dtos.encoder(m)
			.put();
		assertEquals("{\"host\":\"example.com\",\"options\":[\"bar\",\"don\",\"zun\"],\"port\":20}", json);
	}

	@Test
	public void testDiff() throws Exception {
		MyData source = new MyData();
		source.port = 20;
		source.host = "example.com";
		source.options = new Option[] {
			Option.bar, Option.don, Option.zun
		};

		MyData copy = dtos.deepCopy(source);

		assertFalse(source == copy);
		assertTrue(dtos.equals(source, copy));

		List<Difference> diff = dtos.diff(source, copy);
		assertEquals(0, diff.size());

		copy.port = 10;
		diff = dtos.diff(source, copy);
		assertEquals(1, diff.size());
		assertEquals("port", diff.get(0).path[0]);
	}


}
