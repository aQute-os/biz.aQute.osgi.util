package biz.aQute.modbus.api;

public interface Server {
	PDU accept(PDU request);

	/**
	 */
	PDU getPDU();
}
