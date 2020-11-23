package biz.aQute.modbus.api;

public class Response {
	public final ModbusExceptions	code;
	public final String				reason;

	Response(ModbusExceptions code, String reason) {
		this.code = code;
		this.reason = reason;
	}

	public static Response OK = new Response(ModbusExceptions.Ok, "OK");

	public static Response illegalFunction(String reason) {
		if (reason == null)
			reason = "Ilegal Function";
		return new Response(ModbusExceptions.IllegalFunction, reason);
	}

	public static Response illegalDataAddress(String reason) {
		if (reason == null)
			reason = "Ilegal Data Address";
		return new Response(ModbusExceptions.IllegalDataAddress, reason);
	}

	public static Response illegalDataValue(String reason) {
		if (reason == null)
			reason = "Ilegal Data Value";
		return new Response(ModbusExceptions.IllegalDataValue, reason);
	}

	public static Response serverDeviceFailure(String reason) {
		if (reason == null)
			reason = "Server Device Failure";
		return new Response(ModbusExceptions.ServerDeviceFailure, reason);
	}

	public static Response acknowledge() {
		return new Response(ModbusExceptions.Acknowledge, "");
	}

	public static Response serverDeviceBusy(String reason) {
		if (reason == null)
			reason = "Server Device Busy";
		return new Response(ModbusExceptions.ServerDeviceBusy, reason);
	}

	public static Response memoryParityError(String reason) {
		if (reason == null)
			reason = "Memory Parity Error";
		return new Response(ModbusExceptions.MemoryParityError, reason);
	}

	public static Response gatewayPathUnavailable(String reason) {
		if (reason == null)
			reason = "Gateway Path Unavailable";
		return new Response(ModbusExceptions.GatewayPathUnavailable, reason);
	}

	public static Response gatewayTargetDeviceFailedToRespond(String reason) {
		if (reason == null)
			reason = "Gateway Target Device Failed To Respond";
		return new Response(ModbusExceptions.GatewayTargetDeviceFailedToRespond, reason);
	}

	@Override
	public String toString() {
		return "Response [code=" + code + ", reason=" + reason + "]";
	}

}
