package biz.aQute.modbus.api;
public enum ModbusExceptions {
	Ok(0, "Ok", false, false), //
	IllegalFunction(1,
			"Function code received in the query is not recognized or allowed by slave",
			true, false), //
	IllegalDataAddress(2,
			"Data address of some or all the required entities are not allowed or do not exist in slave",
			true, false), //
	IllegalDataValue(3,
			"Value is not accepted by slave", true,
			false), //
	ServerDeviceFailure(4,
			"Unrecoverable error occurred while slave was attempting to perform requested action",
			true, false), //
	Acknowledge(5,
			"Slave has accepted request and is processing it, but a long duration of time is required. This\n"
					+ "response is returned to prevent a timeout error from occurring in the master. Master can next issue a Poll\n"
					+ "Program Complete message to determine if processing is completed",
			false,
			false), //
	ServerDeviceBusy(6,
			"Slave Device Busy Slave is engaged in processing a long-duration command. Master should retry later",
			false,
			false), //
	NegativeAcknowledge(
			7,
			"Slave cannot perform the programming functions. Master should request diagnostic or\n"
					+ "error information from slave",
			false,
			false), //
	MemoryParityError(
			8,
			"Slave detected a parity error in memory. Master can retry the request, but service may be\n"
					+ "required on the slave device",
			false,
			false), //
	GatewayPathUnavailable(
			10,
			"Specialized for Modbus gateways. Indicates a misconfigured gateway",
			false,
			false), //
	GatewayTargetDeviceFailedToRespond(
			11,
			"Specialized for Modbus gateways. Sent when slave fails to respond",
			false,
			false), //
	InvalidResponseCode(
			-1,
			"Response code != Request code",
			true,
			true), //
	Unknown(
			-1,
			"Unknown code",
			true,
			true);

	final byte		code;
	final String	message;
	final boolean	dumpRequest;
	final boolean	dumpResponse;

	ModbusExceptions(int code, String message, boolean dumpRequest,
			boolean dumpResponse) {
		this.code = (byte) code;
		this.message = message;
		this.dumpRequest = dumpRequest;
		this.dumpResponse = dumpResponse;
	}
}

