package biz.aQute.modbus.emulator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

import biz.aQute.modbus.api.MessagingProtocol;
import biz.aQute.modbus.api.PDU;
import biz.aQute.modbus.reflective.Emulator;

public class EmulatorTest {

	static class Foo {
		int		foo	= 76;
		boolean	c1	= false;

		public int getFoo() {
			return foo++;
		}

		public int setFoo(int v) {
			return foo = v;
		}

		public String getBar() {
			return "Hello";
		}

		public int getSigned() {
			return 25;
		}

		public int getNegative() {
			return -25;
		}
	}

	@Test
	public void emulator() throws InterruptedException, IOException {
		Foo foo = new Foo();
		Emulator emulator = new Emulator(//
			"" //
				+ "H:foo:10:i32\n" //
				+ "H:bar:12:String:8\n" //
				+ "C:c1:20\n" //
				+ "H:signed:30:i16\n" //
				+ "H:negative:4000:i16\n",
			foo);

		MessagingProtocol server = new MessagingProtocol(true);
		server.addUnit(1, emulator);

		MessagingProtocol client = new MessagingProtocol(true);

		PDU pdu = client.readHoldingRegisters(server, 1, 12, 8);
		assertThat(pdu.remaining()).isEqualTo(16);
		assertThat(pdu.getString(16)).isEqualTo("Hello");

		pdu = client.readHoldingRegisters(server, 1, 10, 2);
		assertThat(pdu.remaining()).isEqualTo(4);
		assertThat(pdu.getI32()).isEqualTo(76);

		pdu = client.readHoldingRegisters(server, 1, 10, 2);
		assertThat(pdu.getI32()).isEqualTo(77);
		assertThat(foo.foo).isEqualTo(78);

		pdu = client.readHoldingRegisters(server, 1, 12, 8);

		pdu = client.readHoldingRegisters(server, 1, 30, 1);
		assertThat(pdu.getI16()).isEqualTo(25);
		pdu = client.readHoldingRegisters(server, 1, 4000, 1);
		assertThat(pdu.getI16()).isEqualTo(-25);
	}

	public static class Bar {
		public int bar = 1;

		public int getBar() {
			return 2;
		}
	}

	@Test
	public void testMethodPriorityOverField() throws InterruptedException, IOException {
		Bar bar = new Bar();
		Emulator emulator = new Emulator(//
			"" //
				+ "H:bar:0:i32\n" //
			, bar);

		MessagingProtocol server = new MessagingProtocol(true);
		server.addUnit(1, emulator);

		MessagingProtocol client = new MessagingProtocol(true);
		PDU pdu = client.readHoldingRegisters(server, 1, 0, 2);
		assertThat(pdu.getI32()).isEqualTo(2);
	}
}
