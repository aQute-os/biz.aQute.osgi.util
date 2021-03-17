package biz.aQute.modbus.api;

public class Recorder implements Server {
	final Server forward;

	public Recorder(Server server) {
		forward = server;
	}

	@Override
	public PDU accept(PDU request) {
		PDU response = forward.accept(request);
		return response;
	}

	@Override
	public PDU getPDU() {
		return forward.getPDU();
	}

}
