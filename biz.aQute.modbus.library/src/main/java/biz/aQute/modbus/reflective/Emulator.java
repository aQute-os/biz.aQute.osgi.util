package biz.aQute.modbus.reflective;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import aQute.lib.converter.Converter;
import aQute.lib.io.IO;
import biz.aQute.modbus.api.PDU;
import biz.aQute.modbus.api.PDU.Bits;
import biz.aQute.modbus.api.Response;
import biz.aQute.modbus.api.Unit;
import biz.aQute.modbus.reflective.AccessMapper.Access;
import biz.aQute.result.Result;

public class Emulator implements Unit {

	final AccessMapper	access;
	final Reflector		reflector;

	public Emulator(String config, Object... args) throws IOException {
		this(IO.stream(config), args);
	}

	public Emulator(InputStream stream, Object[] args) throws IOException {
		this(new AccessMapper(stream), new Reflector(args));
	}

	public Emulator(AccessMapper accessMapper, Reflector reflector) {
		this.access = accessMapper;
		this.reflector = reflector;
	}

	@Override
	public Response readDiscreteInputs(int start, int length, Bits response) throws Exception {
		return readbits(Bank.I, start, length, response);
	}

	@Override
	public Response readCoils(int start, int length, Bits response) throws Exception {
		return readbits(Bank.C, start, length, response);
	}

	@Override
	public Response writeCoils(Bits bits, int start, int length) {
		int rover = 0;

		while (rover < start + length) {
			Optional<Access> a = access.findAccess(Bank.C, rover + start);
			if (!a.isPresent())
				return Response.illegalDataAddress("Coils " + rover + start);

			boolean b = bits.get();
			Result<Boolean> set = reflector.set(a.get().name,b);
			if ( set.isErr()) {
				return Response.illegalDataValue("Coils " + rover + start + ": " + set);
			}
			rover++;
		}
		return Response.OK;
	}

	@Override
	public Response readInputRegisters(int start, int length, PDU response) throws Exception {
		return readWords(Bank.I, start, length, response);
	}

	@Override
	public Response readHoldingRegisters(int start, int length, PDU response) throws Exception {
		return readWords(Bank.H, start, length, response);
	}

	@Override
	public Response writeHoldingRegisters(PDU buffer, int start, int length) {
		int rover = 0;

		while (rover < length) {
			Optional<Access> oa = access.findAccess(Bank.H, rover + start);
			if (!oa.isPresent())
				return Response.illegalDataAddress("H " + rover + start);

			Access a = oa.get();
			Object v = buffer.get(a.entry);
			Result<Object> set = reflector.set(a.name,v);
			if ( set.isErr())
				return Response.illegalDataValue(set.toString());

			rover += a.entry.width / 2;
		}

		return Response.OK;
	}

	private Response readbits(Bank bank, int start, int length, Bits response) throws Exception {

		int rover = start;

		while (rover < length) {
			Optional<Access> a = access.findAccess(bank, rover + start);
			if (!a.isPresent())
				return Response.illegalDataAddress(bank + " " + rover + start);

			Result<Object> value = reflector.get(a.get().name);
			if (value.isErr())
				return Response.illegalDataAddress(value.toString());

			Boolean b = Converter.cnv(Boolean.class, value.unwrap());
			response.put(b);
			rover += 1;
		}
		return Response.OK;
	}


	private Response readWords(Bank bank, int start, int words, PDU response) throws Exception {
		int rover = 0;

		while (rover < words) {
			Optional<Access> oa = access.findAccess(bank, rover+start);

			if (!oa.isPresent())
				return Response.illegalDataAddress(bank + " " + rover + start);

			Access a = oa.get();

			Result<Object> setget = reflector.get(a.name);
			if (setget.isErr())
				return Response.illegalDataAddress(setget.toString());


			Object v = setget.unwrap();
			response.put(a.entry, v);
			rover += a.entry.width / 2;
		}
		return Response.OK;
	}

}
