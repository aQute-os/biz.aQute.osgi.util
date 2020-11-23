package biz.aQute.modbus.reflective;

import java.util.Optional;

import biz.aQute.modbus.api.ApplicationProtocol;
import biz.aQute.modbus.api.PDU;
import biz.aQute.modbus.api.Server;
import biz.aQute.modbus.reflective.AccessMapper.Access;

public class Client {
	final AccessMapper	mapper;
	final Server		remote;
	final ApplicationProtocol			local;

	public Client(AccessMapper mapper, Server server, ApplicationProtocol adu) {
		this.mapper = mapper;
		this.remote = server;
		this.local = adu;
	}

	public Optional<Object> get(int unit, String name) {
		Access access = mapper.get(name);
		if (access == null)
			return Optional.empty();

		switch (access.bank) {
			case C :
				break;
			case H :
				PDU registers = local.readHoldingRegisters(remote, unit, access.address, access.entry.width);
				return Optional.ofNullable(registers.get(access.entry));

			case I :
				break;
			case R :
				break;
		}

		return Optional.empty();
	}

}
