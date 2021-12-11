package aQute.quantity.types.util;

import aQute.quantity.base.util.BaseQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.Unit.Dimension;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit = "mol", symbol="n", dimension = "Amount of substance", symbolForDimension = "N")
public class Substance extends BaseQuantity<Substance> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Substance.class);
	public static final Dimension		DIMe1				= Unit.dimension(Substance.class, 1);

	Substance(double value) {
		super(value);
	}

	@Override
	protected Substance same(double value) {
		return from(value);
	}

	public static Substance from(double value) {
		return new Substance(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

}
