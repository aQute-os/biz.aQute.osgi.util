package biz.aQute.modbus.reflective;

import biz.aQute.modbus.api.PDU;

public enum Bank {
	I("DiscreteInput", 2000, false, false) {
		@Override
		public int width(PDU.DataType registerType) {
			return 1;
		}
	},
	C("Coil", 2000, true, false) {
		@Override
		public int width(PDU.DataType registerType) {
			return 1;
		}
	},
	R("Register", 0x1_0000, false,
			true) {
		@Override
		public int width(PDU.DataType registerType) {
			return registerType.width;
		}
	},
	H("HoldingRegister", 0x1_0000, true, true) {
		@Override
		public int width(PDU.DataType registerType) {
			return registerType.width;
		}
	};

	public final String		name;
	public final boolean	word;
	public final boolean	rw;
	public final int		max;

	Bank(String name, int max, boolean rw, boolean word) {
		this.name = name;
		this.rw = rw;
		this.word = word;
		this.max = max;
	}

	public abstract int width(PDU.DataType registerType);
}
