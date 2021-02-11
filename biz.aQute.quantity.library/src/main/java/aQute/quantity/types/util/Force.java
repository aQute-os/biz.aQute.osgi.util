package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit="N", symbol="F", dimension="Force", symbolForDimension="")
public class Force extends DerivedQuantity<Force>{

	private static final long serialVersionUID = 1L;

	private static Unit unit = new Unit(Force.class, Mass.DIMe1, Length.DIMe1, Time.DIMe_2);

	public Force(double value) {
		super(value);
	}

	@Override
	protected Force same(double value) {
		return from(value);
	}

	public static Force from(double value) {
		return new Force(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}
}
