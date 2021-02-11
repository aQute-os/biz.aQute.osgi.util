package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit="Gy/s", dimension = "Absorbed dose", symbol = "", symbolForDimension = "" )
public class AbsorbedDose extends DerivedQuantity<AbsorbedDose>{
	private static final long serialVersionUID = 1;
	private static Unit unit = new Unit(AbsorbedDose.class, Length.DIMe2, Time.DIMe_3);

	public AbsorbedDose(double value) {
		super(value);
	}

	@Override
	protected AbsorbedDose same(double value) {
		return AbsorbedDose.fromGray(value);
	}

	public static AbsorbedDose fromGray(double value) {
		return new AbsorbedDose(value);
	}
	
	public double toGray() {
		return value;
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

}
