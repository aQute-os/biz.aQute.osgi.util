package de.sma.fieldbus.modbus.master.mapping.provider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.promise.Promise;

import de.sma.fieldbus.metadata.mapper.util.MetadataMapper.Mapper;
import de.sma.fieldbus.modbus.master.mapping.provider.ModbusDevice;
import de.sma.fieldbus.modbus.master.mapping.provider.ModbusDriver;
import de.sma.fieldbus.modbus.master.mapping.provider.ModbusMapper;
import de.sma.fieldbus.modbus.master.mapping.provider.ModbusMapper.ModbusMap;
import de.sma.iguana.promises.util.ThreadSwitcher;
import de.sma.iguana.test.util.DummyCatalogAdmin;
import de.sma.iguana.test.util.DummyDTOs;
import de.sma.iguana.test.util.DummyDataAdmin;
import de.sma.iguana.test.util.DummyComDispatcher;
import de.sma.iguana.test.util.DummyFramework;
import de.sma.iguana.test.util.DummyScheduler;
import de.sma.iguana.test.util.TraceParser;
import de.sma.pollingadmin.api.Pollable;

public class ModbusDriverTest extends Assert {

	ModbusDriver		driver			= new ModbusDriver();

	ThreadSwitcher		switcher		= new ThreadSwitcher();
	DummyFramework		framework		= new DummyFramework();
	DummyDataAdmin		dataAdmin		= new DummyDataAdmin();
	DummyComDispatcher	dispatcher		= new DummyComDispatcher(switcher);
	DummyCatalogAdmin	catalogAdmin	= new DummyCatalogAdmin();
	DummyScheduler		scheduler		= new DummyScheduler();
	DummyDTOs			dtos			= new DummyDTOs();
	TraceParser			tracer;

	@Before
	public void setUp() throws Exception {
		tracer = new TraceParser(ModbusDriverTest.class
				.getResourceAsStream("modbus-driver-test.txt"));

		driver.dataAdmin = dataAdmin;
		driver.dispatcher = dispatcher;
		driver.scheduler = scheduler;
		driver.catalogAdmin = catalogAdmin;
		driver.dtos = dtos;

		driver.mapper = mock(ModbusMapper.class);

		driver.activate(framework.context);
	}

	@After
	public void tearDown() throws Exception {
		framework.close();
		driver.deactivate();
	}

	static class DummyMapper implements Mapper<ModbusMap> {

		final public String		name;
		final public ModbusMap	map;

		public DummyMapper(String name, URL resource) throws Exception {
			this.name = name;
			map = ModbusMapper.parse0(resource);
		}

		public DummyMapper(String name, String json) throws Exception {
			this.name = name;
			byte[] data = json.getBytes(StandardCharsets.UTF_8);
			ByteArrayInputStream bin = new ByteArrayInputStream(data);
			this.map = ModbusMapper.parse0(bin);
		}

		@Override
		public ModbusMap get() {
			try {
				return map;
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public String getName() {
			return name;
		}

	}

	ModbusDevice setup(String name) throws Exception {
		dispatcher.matcher = tracer.getMatcher(name + "-trace");
		dispatcher.response = driver;

		String mapping = tracer.getProperty(name + "-mapping");
		when(driver.mapper.getMapper(name))
				.thenReturn(new DummyMapper(name, mapping));

		ManagedServiceFactory msf = framework
				.getService(ManagedServiceFactory.class);

		Hashtable<String, Object> config = new Hashtable<>();
		config.put("route", "modbus.test:1");
		config.put("mapping", name);
		msf.updated("1", config);

		ModbusDevice d1 = (ModbusDevice) framework.getService(Pollable.class);
		assertNotNull("Should have registered a pollable device", d1);
		Promise<Void> update = d1.update(0xFFFF);

		update.onResolve(() -> System.out.println("done"));

		switcher.flush();
		update.getValue();
		return d1;
	}

	@Test
	public void testGet16ByteString() throws Exception {
		ModbusDevice d1 = setup("device-simple");

		assertEquals("XYZ             ",
				(String) dataAdmin.getJavaValue(d1.deviceId, "string", "PV"));
	}

	@Test
	public void testBigEndianWordAndLong() throws Exception {
		ModbusDevice d1 = setup("big-endian");
		assertEquals((Integer) 1,
				(Integer) dataAdmin.getJavaValue(d1.deviceId, "int32", "PV"));
		assertEquals((Long) 2L,
				(Long) dataAdmin.getJavaValue(d1.deviceId, "int64", "PV"));
	}

	@Test
	public void testLittleEndianWordAndLong() throws Exception {
		ModbusDevice d1 = setup("little-endian");
		assertEquals((Integer) 1,
				(Integer) dataAdmin.getJavaValue(d1.deviceId, "int32", "PV"));
		assertEquals((Long) 2L,
				(Long) dataAdmin.getJavaValue(d1.deviceId, "int64", "PV"));
	}

	@Test
	public void testPacketTooLong() throws Exception {
		try {
			setup("packet-too-long");
			fail("Exepected an exception that the packet length was too long");
		} catch (InvocationTargetException e) {
			assertTraceContains(e, "the ADU is longer than the maximum length");
		}
	}

	@Test
	public void testPacketWrongResponseCode() throws Exception {
		try {
			setup("packet-wrong-response-code");
			fail("Exepected an exception that the response code was a wrong code");
		} catch (InvocationTargetException e) {
			assertTraceContains(e, "Response code != Request code");
		}
	}

	@Test
	public void testIllegalFunctionException() throws Exception {
		try {
			setup("illegal-function");
			fail("Exepected an exception we sent an illegal function");
		} catch (InvocationTargetException e) {
			assertTraceContains(e,
					"Function code received in the query is not recognized or allowed by slave");
		}
	}

	@Test
	public void testAck() throws Exception {
		try {
			setup("ack");
			fail("Exepected an exception we sent an illegal function");
		} catch (InvocationTargetException e) {
			assertTraceContains(e, "Acknowledge");
		}
	}

	@Test
	public void testNoResponse() throws Exception {
		try {
			driver.maxResponseTime = 2000;
			setup("no-response");
			fail("Timeout");
		} catch (InvocationTargetException e) {
			assertTraceContains(e, "TimeoutException");
		}
	}

	@Test
	public void testInvalidLengthInResponse() throws Exception {
		try {
			setup("invalid-length");
			fail("expected exception");
		} catch (InvocationTargetException e) {
			assertTraceContains(e, "BufferUnderflowException");
		}
	}

	private void assertTraceContains(InvocationTargetException e,
			String string) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.close();
		if (sw.toString().contains(string))
			return;

		fail(" Expected \"" + string + "\" in " + sw);
	}

}
