package biz.aQute.modbus.api;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class models a ModBus Application Protocol wrapper. It inserts the MBAP
 * header in front of the Modbus ADU.
 */
public class MessagingProtocol extends ApplicationProtocol implements Server {

	public static final int			MBAP_LENGTH	= 7;						// includes
																			// unit
	final AtomicInteger				transaction	= new AtomicInteger(1000);

	public MessagingProtocol(boolean bigWordEndian) {
		super(bigWordEndian);
	}


	@Override
	public PDU send(PDU buffer, Server transport) {
		buffer.seal();
		buffer.putU16(4, buffer.limit() - 6);
		return transport.accept(buffer);
	}

	@Override
	public PDU getPDU() {

		return new PDU.Builder().bigByteEndian(true)
			.bigWordEndian(bigWordEndian)
			.size(MBAP_LENGTH + ApplicationProtocol.MAXIMUM_ADU_LENGTH)
			.build();
	}

	@Override
	public PDU getRequestPDU() {
		PDU pdu = getPDU();
		pdu.putU16(transaction.getAndIncrement());
		pdu.putU16(0);
		pdu.putU16(0);
		return pdu;
	}

	@Override
	public PDU accept(PDU buffer) {
		PDU response = getPDU();
		response.putU16(buffer.getU16()); // transaction
		response.putU16(buffer.getU16()); // protocol
		int length = buffer.getU16();
		assert length > 0 && length < ApplicationProtocol.MAXIMUM_ADU_LENGTH;
		response.putU16(0); // length response
		int unit = buffer.getU8();
		response.putU8(unit);
		indication(buffer, unit, response);
		response.seal();
		response.putU16(4, response.limit - 6);
		response.position(7);
		return response;
	}

}
