package biz.aQute.modbus.api;

import java.io.Closeable;

import biz.aQute.modbus.api.PDU.Bits;

public interface Unit {


	Response readDiscreteInputs(int start, int length, Bits bits) throws Exception;

	Response readCoils(int start, int length, Bits bits) throws Exception;

	Response writeCoils(Bits bits, int start, int length) throws Exception;

	Response readInputRegisters(int start, int length, PDU response) throws Exception;

	Response readHoldingRegisters(int start, int length, PDU response) throws Exception;

	Response writeHoldingRegisters(PDU buffer, int start, int length) throws Exception;

	default Closeable begin() {
		return () -> {
		};
	}
}
