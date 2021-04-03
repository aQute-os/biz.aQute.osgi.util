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

		public void setBar(short v) {
			this.bar = v;
		}
	}

	@Test
	public void testMethodPriorityOverField() throws Exception {
		Bar bar = new Bar();
		Emulator emulator = new Emulator(//
			"" //
				+ "H:bar:0:i16\n" //
			, bar);

		MessagingProtocol server = new MessagingProtocol(true);
		server.addUnit(1, emulator);

		MessagingProtocol client = new MessagingProtocol(true);
		PDU pdu = client.readHoldingRegisters(server, 1, 0, 1);
		assertThat(pdu.getI16()).isEqualTo(2);

		pdu = client.writeMultipleRegisters(server, 1, 0, 5);
		assertThat(bar.bar).isEqualTo(5);

	}

	public static class WithArray {
		public int[] bar = new int[] {
			1, 2, 3
		};
	}

	@Test
	public void testArrays() throws Exception {
		WithArray bar = new WithArray();
		Emulator emulator = new Emulator(//
			"" //
				+ "H:bar:0:i32:6\n" //
			, bar);

		MessagingProtocol server = new MessagingProtocol(true);
		server.addUnit(1, emulator);

		MessagingProtocol client = new MessagingProtocol(true);
		PDU pdu = client.readHoldingRegisters(server, 1, 0, 6);
		assertThat(pdu.getI32()).isEqualTo(1);
		assertThat(pdu.getI32()).isEqualTo(2);
		assertThat(pdu.getI32()).isEqualTo(3);

		pdu = client.writeMultipleRegisters(server, 1, 0, 0, 4, 0, 5, 0, 6);
		assertThat(bar.bar).isEqualTo(new int[] {
			4, 5, 6
		});

	}

	@Test
	public void testDoubleArrays() throws Exception {
		WithArray bar = new WithArray();
		Emulator emulator = new Emulator(//
			"" //
				+ "H:bar:0:Double:6\n" //
			, bar);

		MessagingProtocol server = new MessagingProtocol(true);
		server.addUnit(1, emulator);

		MessagingProtocol client = new MessagingProtocol(true);
		PDU pdu = client.readHoldingRegisters(server, 1, 0, 12);
		assertThat(pdu.getDouble()).isEqualTo(1D);
		assertThat(pdu.getDouble()).isEqualTo(2D);
		assertThat(pdu.getDouble()).isEqualTo(3D);

		pdu = client.writeMultipleRegisters(server, 1, 0, 0, 4, 0, 0, 0, 5, 0, 0, 0, 0, 0, 6);
		assertThat(bar.bar).isEqualTo(new int[] {
			4, 5, 6
		});

	}
}
