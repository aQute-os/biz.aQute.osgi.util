package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit="var", symbol="Q", dimension = "Reactive Power", symbolForDimension = "")
public class ReactivePower extends DerivedQuantity<ReactivePower>{
	private static final long serialVersionUID = 1L;
	
	Unit		unit = new Unit(ReactivePower.class, Mass.DIMe1, Length.DIMe2, Time.DIMe_3);
	
	ReactivePower(double value) {
		super(value);
	}

	public static ReactivePower from( double value) {
		return new ReactivePower(value);
	}
	
	@Override
	protected ReactivePower same(double value) {
		return from(value);
	}
	
	public static ReactivePower fromVA( ElectricPotential uRms, Current iRms, Radian phi) {
		return from( uRms.value * iRms.value * phi.sin());
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

}
