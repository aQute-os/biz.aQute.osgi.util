package biz.aQute.modbus.ip;

import java.io.IOException;

import org.junit.Test;

import biz.aQute.modbus.api.PDU;
import biz.aQute.modbus.api.PDU.Bits;
import biz.aQute.modbus.api.Response;
import biz.aQute.modbus.api.Unit;

public class ModbusTCPTest {

	byte data = (byte) 0x40;

	@Test
	public void simple() throws InterruptedException, IOException {
		// MessagingProtocol server = new MessagingProtocol(true);
		// server.addUnit(1, getUnit(1026));
		//
		// ModbusTCP transport = new ModbusTCP(1026, null, server);
		// transport.start();
		//
		// Socket s = new Socket("localhost", 1026);
		//
		// MessagingProtocol mp = new MessagingProtocol(true);
		// Server client = new Server() {
		//
		// @Override
		// public PDU accept(PDU request) {
		// try {
		// request.write(s.getOutputStream());
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// return null;
		// }
		//
		// @Override
		// public PDU getPDU() {
		// // TODO Auto-generated method stub
		// return null;
		// }
		//
		// };
		// mp.readHoldingRegisters(client, 1, 0, 1);
		//
		//
	}

	Unit getUnit(int n) {
		Unit slave = new Unit() {

			@Override
			public Response writeHoldingRegisters(PDU buffer, int start, int length) throws Exception {
				System.out.println(n + " writeholdingregs " + buffer + " start=" + start + " length=" + length);
				return Response.OK;
			}

			@Override
			public Response writeCoils(Bits bits, int start, int length) throws Exception {
				System.out.println(n + " coils " + bits + " start=" + start + " length=" + length);
				return Response.OK;
			}

			@Override
			public Response readInputRegisters(int start, int length, PDU response) throws Exception {
				System.out.println(n + " read input regs  start=" + start + " length=" + length);
				response.putU16(length);
				for (int i = 0; i < length; i++) {
					response.putU16(i);
				}
				return Response.OK;
			}

			@Override
			public Response readHoldingRegisters(int start, int length, PDU response) throws Exception {
				System.out.println(n + " read holding regs  start=" + start + " length=" + length);
				for (int i = 0; i < length; i++) {
					response.putU16(i);
				}
				return Response.OK;
			}

			@Override
			public Response readDiscreteInputs(int start, int length, Bits bits) throws Exception {
				System.out.println(n + " read discrete inputs  start=" + start + " length=" + length);
				for (int i = 0; i < length; i++) {
					bits.put(i, true);
				}
				return Response.OK;
			}

			@Override
			public Response readCoils(int start, int length, Bits bits) throws Exception {
				System.out.println("read coils  start=" + start + " length=" + length);
				for (int i = 0; i < length; i++) {
					bits.put(i, false);
				}
				return Response.OK;
			}
		};
		return slave;
	}

	static class Foo {
		int foo = 76;

		public int getFoo() {
			return foo++;
		}

		public String getBar() {
			return "Hello";
		}

	}
}
