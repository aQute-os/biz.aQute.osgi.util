package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit="Wb", symbol="Φ", dimension="Magnetic Flux", symbolForDimension="")
public class Flux extends DerivedQuantity<Flux> {
	private static final long		serialVersionUID	= 1;
	private static final Unit	dimension			= new Unit(Flux.class);

	Flux(double value) {
		super(value);
	}

	@Override
	protected Flux same(double value) {
		return from(value);
	}

	private Flux from(double value) {
		return new Flux(value);
	}

	@Override
	public Unit getUnit() {
		return dimension;
	}

}
