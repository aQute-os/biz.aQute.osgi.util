package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit="T", symbol="B", dimension="Magnetic Field", symbolForDimension="")
public class MagneticField extends DerivedQuantity<MagneticField>{
	private static final long serialVersionUID = 1L;
	private static final Unit unit = new Unit(MagneticField.class, Mass.DIMe1, Current.DIMe_1, Time.DIMe_2);

	public MagneticField(double value) {
		super(value);
	}

	@Override
	protected MagneticField same(double value) {
		return from(value);
	}

	public static MagneticField from(double value) {
		return new MagneticField(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

}
