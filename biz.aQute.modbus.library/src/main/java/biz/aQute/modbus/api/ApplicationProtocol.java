package biz.aQute.modbus.api;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.aQute.modbus.api.PDU.Bits;
import biz.aQute.modbus.api.PDU.BlockLength;

/**
 * This class provides the commands for the Modbus ADU. The ADO is a structure
 * with a command and then command specific parameters.
 */
public abstract class ApplicationProtocol {
	static Logger				logger				= LoggerFactory.getLogger(ApplicationProtocol.class);

	public final static int		MINIMUM_ADU_LENGTH	= 1;
	public final static int		MAXIMUM_ADU_LENGTH	= 255;
	public final boolean		bigWordEndian;

	final Map<Integer, Unit>	units				= new ConcurrentHashMap<>();

	public ApplicationProtocol(boolean bigWordEndian) {
		this.bigWordEndian = bigWordEndian;
	}

	public Closeable addUnit(int address, Unit unit) {
		units.put(address, unit);
		return () -> {
			units.remove(address);
		};
	}

	/**
	 * Check if the buffer is a valid Application DU
	 */
	public String validate(PDU buffer) {
		return null;
	}

	public boolean indication(PDU buffer, int address, PDU response) {

		int function = buffer.getU8();
		int pos = response.position();

		Response result;

		Unit unit = units.get(address);
		if (unit != null) {
			try {
				try (Closeable closeable = unit.begin()) {
					result = indication(buffer, unit, response, function);
				}
			} catch (Exception e) {
				result = Response.serverDeviceFailure("exception " + e.getMessage());
			}
		} else {
			result = Response.gatewayPathUnavailable("no such unit " + address);
		}
		if ((result != Response.OK)) {
			response.position(pos);
			response.putU8(function | 0x80);
			response.putU8(result.code.code);
			logger.info("modbus exception {}", result);
		}

		return true;
	}

	private Response indication(PDU buffer, Unit slave, PDU response, int function) {
		try (AutoCloseable c = slave.begin()) {
			switch (function) {
				case 0x01 :
					return readCoils(buffer, slave, response);

				case 0x02 :
					return readDiscreteInputs(buffer, slave, response);

				case 0x03 :
					return readHoldingRegisters(buffer, slave, response);

				case 0x04 :
					return readInputRegisters(buffer, slave, response);

				case 0x05 :
					return writeSingleCoil(buffer, slave, response);

				case 0x06 :
					return writeSingleRegister(buffer, slave, response);

				case 0x10 :
					return writeMultipleRegisters(buffer, slave, response);

				default :
					return Response.illegalFunction("no such function " + function);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverDeviceFailure("exception " + e.getMessage());
		}
	}

	/**
	 * 01 (0x01) Read Coils This function code is used to read from 1 to 2000
	 * contiguous status of coils in a remote device. The Request PDU specifies
	 * the starting address, i.e. the address of the first coil specified, and
	 * the number of coils. In the PDU Coils are addressed starting at zero.
	 * Therefore coils numbered 1-16 are addressed as 0-15. The coils in the
	 * response message are packed as one coil per bit of the data field. Status
	 * is indicated as 1= ON and 0= OFF. The LSB of the first data byte contains
	 * the output addressed in the query. The other coils follow toward the high
	 * order end of this byte, and from low order to high order in subsequent
	 * bytes. If the returned output quantity is not a multiple of eight, the
	 * remaining bits in the final data byte will be padded with zeros (toward
	 * the high order end of the byte). The Byte Count field specifies the
	 * quantity of complete bytes of data.
	 *
	 * @param buffer
	 * @param impl
	 * @param response
	 * @return
	 * @throws Exception
	 */
	Response readCoils(PDU buffer, Unit impl, PDU response) throws Exception {

		int start = buffer.getU16();
		int length = buffer.getU16();

		response.putU8(0x01);
		BlockLength coils = response.putBlockLengthU8();

		Response r = impl.readCoils(start, length, response.putBits(length));
		if (r != Response.OK) {
			return r;
		}
		int l = coils.close();
		if (l > MAXIMUM_ADU_LENGTH - 2)
			return Response.illegalDataValue("Too many coils for packet " + length);

		return Response.OK;
	}

	public PDU readCoils(Server transport, int unit, int start, int length) {
		PDU request = getRequestPDU();
		request.putU8(unit)
			.putU8(0x01)
			.putU16(start)
			.putU16(length);

		PDU response = send(request, transport);
		int function = response.getU8();
		if (function != 0x01)
			throw new IllegalArgumentException("exception " + response);

		int l = response.getU8();
		assert l == response.limit() - response.position() : "must match";

		return response;
	}

	/**
	 * 02 (0x02) Read Discrete Inputs This function code is used to read from 1
	 * to 2000 contiguous status of discrete inputs in a remote device. The
	 * Request PDU specifies the starting address, i.e. the address of the first
	 * input specified, and the number of inputs. In the PDU Discrete Inputs are
	 * addressed starting at zero. Therefore Discrete inputs numbered 1-16 are
	 * addressed as 0-15. The discrete inputs in the response message are packed
	 * as one input per bit of the data field. Status is indicated as 1= ON; 0=
	 * OFF. The LSB of the first data byte contains the input addressed in the
	 * query. The other inputs follow toward the high order end of this byte,
	 * and from low order to high order in subsequent bytes. If the returned
	 * input quantity is not a multiple of eight, the remaining bits in the
	 * final data byte will be padded with zeros (toward the high order end of
	 * the byte). The Byte Count field specifies the quantity of complete bytes
	 * of data.
	 *
	 * @param buffer
	 * @param impl
	 * @param response
	 * @throws Exception
	 */
	Response readDiscreteInputs(PDU buffer, Unit impl, PDU response) throws Exception {

		int start = buffer.getU16();
		int length = buffer.getU16();

		response.putU8(0x02);
		BlockLength coils = response.putBlockLengthU8();

		Response r = impl.readDiscreteInputs(start, length, response.putBits(length));
		if (r != Response.OK) {
			return r;
		}
		int l = coils.close();
		if (l > MAXIMUM_ADU_LENGTH - 2)
			return Response.illegalDataValue("Too many coils for packet " + length);

		return Response.OK;
	}

	public PDU readDiscreteInputs(Server transport, int unit, int start, int length) {
		PDU request = getRequestPDU();
		request.putU8(unit)
			.putU8(0x02)
			.putU16(start)
			.putU16(length);

		PDU response = send(request, transport);
		int function = response.getU8();
		if (function != 0x01)
			throw new IllegalArgumentException("exception " + response);

		int l = response.getU8();
		assert l == response.limit() - response.position() : "must match";

		return response;
	}

	/**
	 * 03 (0x03) Read Holding Registers
	 * <p>
	 * This function code is used to read the contents of a contiguous block of
	 * holding registers in a remote device. The Request PDU specifies the
	 * starting register address and the number of registers. In the PDU
	 * Registers are addressed starting at zero. Therefore registers numbered
	 * 1-16 are addressed as 0-15. The register data in the response message are
	 * packed as two bytes per register, with the binary contents right
	 * justified within each byte. For each register, the first byte contains
	 * the high order bits and the second contains the low order bits.
	 *
	 * @param buffer
	 * @param impl
	 * @return
	 * @throws Exception
	 */

	Response readHoldingRegisters(PDU buffer, Unit impl, PDU response) throws Exception {
		int start = buffer.getU16();
		int length = buffer.getU16();

		response.putU8(0x03);
		BlockLength registers = response.putBlockLengthU8();

		Response r = impl.readHoldingRegisters(start, length, response);
		if (r != Response.OK) {
			return r;
		}
		int l = registers.close();
		if (l > MAXIMUM_ADU_LENGTH - 2)
			return Response.illegalDataValue("Too many coils for packet " + length);

		return Response.OK;
	}

	public PDU readHoldingRegisters(Server transport, int unit, int start, int length) {
		PDU request = getRequestPDU();
		request.putU8(unit)
			.putU8(0x03)
			.putU16(start)
			.putU16(length);

		PDU response = send(request, transport);
		int function = response.getU8();
		if (function != 0x03)
			throw new IllegalArgumentException("exception " + response);

		int l = response.getU8();
		assert l == response.limit() - response.position() : "must match";

		return response;
	}

	protected abstract PDU getRequestPDU();

	protected abstract PDU send(PDU buffer, Server server);

	/**
	 * 04 (0x04) Read Input Registers This function code is used to read from 1
	 * to 125 contiguous input registers in a remote device. The Request PDU
	 * specifies the starting register address and the number of registers. In
	 * the PDU Registers are addressed starting at zero. Therefore input
	 * registers numbered 1-16 are addressed as 0-15. The register data in the
	 * response message are packed as two bytes per register, with the binary
	 * contents right justified within each byte. For each register, the first
	 * byte contains the high order bits and the second contains the low order
	 * bits.
	 *
	 * @param buffer
	 * @param impl
	 * @throws Exception
	 */
	Response readInputRegisters(PDU buffer, Unit impl, PDU response) throws Exception {
		int start = buffer.getU16();
		int length = buffer.getU16();

		response.putU8(0x04);
		BlockLength coils = response.putBlockLengthU8();

		Response r = impl.readInputRegisters(start, length, response);
		if (r != Response.OK) {
			return r;
		}
		int l = coils.close();
		if (l > MAXIMUM_ADU_LENGTH - 2)
			return Response.illegalDataValue("Too many coils for packet " + length);

		return Response.OK;
	}

	/**
	 * 05 (0x05) Write Single Coil This function code is used to write a single
	 * output to either ON or OFF in a remote device. The requested ON/OFF state
	 * is specified by a constant in the request data field. A value of FF 00
	 * hex requests the output to be ON. A value of 00 00 requests it to be OFF.
	 * All other values are illegal and will not affect the output. The Request
	 * PDU specifies the address of the coil to be forced. Coils are addressed
	 * starting at zero. Therefore coil numbered 1 is addressed as 0. The
	 * requested ON/OFF state is specified by a constant in the Coil Value
	 * field. A value of 0XFF00 requests the coil to be ON. A value of 0X0000
	 * requests the coil to be off. All other values are illegal and will not
	 * affect the coil.
	 *
	 * @param buffer
	 * @param impl
	 * @param response
	 * @throws Exception
	 */
	Response writeSingleCoil(PDU buffer, Unit impl, PDU response) throws Exception {

		int start = buffer.getU16();
		int value = buffer.getU16();

		response.putU8(0x05);

		Bits bits = new PDU(1).putBits(1);
		bits.put(value == 0xFF00);
		bits.flip();

		Response r = impl.writeCoils(bits, start, 1);
		if (r != Response.OK) {
			return r;
		}

		response.putU16(start);
		response.putU16(value);
		return Response.OK;
	}

	/**
	 * 06 (0x06) Write Single Register This function code is used to write a
	 * single holding register in a remote device. The Request PDU specifies the
	 * address of the register to be written. Registers are addressed starting
	 * at zero. Therefore register numbered 1 is addressed as 0. The normal
	 * response is an echo of the request, returned after the register contents
	 * have been written.
	 *
	 * @param buffer
	 * @param impl
	 * @param response
	 * @return
	 * @throws Exception
	 */
	Response writeSingleRegister(PDU buffer, Unit impl, PDU response) throws Exception {

		int start = buffer.getU16();

		// we read the value for the
		// response but we fake a multiple
		// write holding registers so we do not
		// progress the buffer pointer

		int value = buffer.getU16(buffer.position());

		Response rsp = impl.writeHoldingRegisters(buffer, start, 1);
		if (rsp != Response.OK) {
			return rsp;
		}
		response.putU8(0x06);
		response.putU16(start);
		response.putU16(value);

		return Response.OK;
	}

	public PDU writeSingleRegister(Server transport, int unit, int start, int value) throws Exception {
		PDU request = getRequestPDU();
		request.putU8(unit)
			.putU8(0x06)
			.putU16(start)
			.putU16(value);

		PDU response = send(request, transport);
		int function = response.getU8();
		if (function != 0x06)
			throw new IllegalArgumentException("exception " + response);

		int startx = response.getU16();
		assert startx == start : "writeSingleRegister does not return required register address: " + start + " != " + startx;
		int valuex = response.getU16();
		assert valuex == value : "writeSingleRegister does not return required value: " + value + " != " + valuex;

		return response;
	}

	/**
	 * 6.11 15 (0x0F) Write Multiple Coils This function code is used to force
	 * each coil in a sequence of coils to either ON or OFF in a remote device.
	 * The Request PDU specifies the coil references to be forced. Coils are
	 * addressed starting at zero. Therefore coil numbered 1 is addressed as 0.
	 * The requested ON/OFF states are specified by contents of the request data
	 * field. A logical '1' in a bit position of the field requests the
	 * corresponding output to be ON. A logical '0' requests it to be OFF. The
	 * normal response returns the function code, starting address, and quantity
	 * of coils forced.
	 *
	 * @param buffer
	 * @param impl
	 * @return
	 * @throws Exception
	 */
	Response writeMultipleCoils(PDU buffer, Unit impl, PDU response) throws Exception {
		int start = buffer.getU16();
		int length = buffer.getU16();

		buffer.getU8(); // byte count

		Response r = impl.writeCoils(buffer.putBits(length), start, length);
		if (r != Response.OK) {
			return r;
		}

		response.putU8(0x0F);
		response.putU16(start);
		response.putU16(length);
		return Response.OK;
	}

	/**
	 * 6.12 16 (0x10) Write Multiple registers This function code is used to
	 * write a block of contiguous registers (1 to 123 registers) in a remote
	 * device. The requested written values are specified in the request data
	 * field. Data is packed as two bytes per register. The normal response
	 * returns the function code, starting address, and quantity of registers
	 * written.
	 *
	 * @throws Exception
	 */
	Response writeMultipleRegisters(PDU buffer, Unit impl, PDU response) throws Exception {

		int start = buffer.getU16();
		int length = buffer.getU16();

		int byteCount = buffer.getU8();
		int pos = buffer.position();
		Response r = impl.writeHoldingRegisters(buffer, start, length);
		if (r != Response.OK)
			return r;

		assert pos + byteCount == buffer.position();

		response.putU8(0x10);
		response.putU16(start);
		response.putU16(length);
		return Response.OK;
	}

	public PDU writeMultipleRegisters(Server transport, int unit, int start, int... value) throws Exception {
		PDU request = getRequestPDU();
		request.putU8(unit)
			.putU8(0x10)
			.putU16(start)
			.putU16(value.length)
			.putU8(value.length * 2);

		for (int i = 0; i < value.length; i++) {
			request.putU16(value[i]);
		}

		PDU response = send(request, transport);
		int function = response.getU8();
		if (function != 0x10)
			throw new IllegalArgumentException("exception " + response);

		int startx = response.getU16();
		int quantityx = response.getU16();
		if (startx != start)
			throw new IllegalArgumentException("start register not the same " + response);
		if (quantityx != value.length)
			throw new IllegalArgumentException("quantity not the same " + response);

		return response;
	}

	public Set<Integer> getUnitAddresses() {
		return units.keySet();
	}

}
