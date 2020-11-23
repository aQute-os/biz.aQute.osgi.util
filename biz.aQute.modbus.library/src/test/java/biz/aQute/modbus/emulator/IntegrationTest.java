package biz.aQute.modbus.emulator;

import org.junit.Test;

import biz.aQute.modbus.ip.ModbusTCP;
import biz.aQute.modbus.reflective.Emulator;

public class IntegrationTest {

	public static class Inverter {
		public int getRealPowerKW() {
			System.out.println("read Inverter.realPowerKW");
			return 25;
		}
	}

	static class Battery {
		public int getStateOfChargeKWh() {
			System.out.println("read Battery.stateOfChargeKWh");
			return 95;
		}
	}

	@Test
	public void basic() throws Exception {
		Inverter inverter = new Inverter();
		Emulator e1 = new Emulator("H:realPowerKw:0:u16", inverter);

		Battery battery = new Battery();
		Emulator e2 = new Emulator("H:stateOfChargeKWh:0:u16", battery);


		try (ModbusTCP mb1 = new ModbusTCP(1025, null, true)) {
			mb1.addUnit(1, e1);
			mb1.start();

			try (ModbusTCP mb2 = new ModbusTCP(1026, null, true)) {
				mb2.addUnit(1, e2);
				mb2.start();

				// Thread.sleep(10000000);
			}
		}
	}

}
