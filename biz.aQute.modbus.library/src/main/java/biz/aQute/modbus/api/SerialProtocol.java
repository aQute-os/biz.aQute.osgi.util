package biz.aQute.modbus.api;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class models a ModBus Application Protocol wrapper. It inserts the MBAP
 * header in front of the Modbus ADU.
 */
public class SerialProtocol extends ApplicationProtocol implements Server {
	final static int	OVERHEAD	= 4;
	final int			address;
	final Unit			unit;
	final AtomicInteger	crcErrors	= new AtomicInteger();

	public SerialProtocol(boolean bigWordEndian, int address, Unit unit) {
		super(bigWordEndian);
		this.address = address;
		this.unit = unit;
	}

	@Override
	public PDU accept(PDU request) {

		int rcrc = request.getU8(request.limit() - 2) | (request.getU8(request.limit() - 1) << 8);
		int ccrc = crc(request, 0, request.limit() - 2);
		if (rcrc != ccrc) {
			return null;
		}

		PDU response = getPDU();
		int address = request.getU8();
		if (address == 0) {
			int start = request.position();

			for (int u : super.getUnitAddresses()) {
				request.position(start);
				response.reset();
				response.putU8(address);
				indication(request, address, response);
			}
			return null;
		} else {
			response.putU8(address);
			if (indication(request, address, response)) {
				int crc = crc(response, 0, request.limit());
				response.putU8(crc & 0xFF);
				response.putU8((crc >> 8) & 0xFF);
				response.seal();
				response.position(1);
				return response;
			}
			return null;
		}
	}

	private int crc(PDU response, int start, int length) {
		int crc = -1;
		for (int i = 0; i < response.position(); i++) {
			crc ^= response.getU8(i);
			crc <<= 1;
		}
		return crc;
	}

	@Override
	public PDU getPDU() {
		return new PDU.Builder().bigByteEndian(true)
			.bigWordEndian(bigWordEndian)
			.size(OVERHEAD + ApplicationProtocol.MAXIMUM_ADU_LENGTH)
			.build();
	}

	@Override
	protected PDU getRequestPDU() {
		return getPDU();
	}

	@Override
	protected PDU send(PDU buffer, Server server) {
		return server.accept(buffer);
	}

}
