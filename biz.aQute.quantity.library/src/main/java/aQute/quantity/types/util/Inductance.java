package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit = "H", symbol="L", dimension = "Inductance", symbolForDimension = "L")
public class Inductance extends DerivedQuantity<Inductance> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Inductance.class, Mass.DIMe1, Length.DIMe2, Time.DIMe_2,
			Current.DIMe_2);

	Inductance(double value) {
		super(value);
	}

	@Override
	protected Inductance same(double value) {
		return Inductance.from(value);
	}

	private static Inductance from(double value) {
		return new Inductance(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	
}
