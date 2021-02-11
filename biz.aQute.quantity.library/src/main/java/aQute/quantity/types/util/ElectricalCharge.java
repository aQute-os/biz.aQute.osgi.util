package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

/**
 * The SI system defines the coulomb in terms of the ampere and second: 1 C = 1
 * A × 1 s.
 * 
 * Since the charge of one electron is known to be about −1.6021766208(98)×10−19
 * C,[7] −1 C can also be considered the charge of roughly 6.241509×1018
 * electrons (or +1 C the charge of that many positrons or protons), where the
 * number is the reciprocal of 1.602177×10−19.
 * 
 */
@UnitInfo(unit = "C", symbol = "Q", dimension = "Electric charge", symbolForDimension = "?")
public class ElectricalCharge extends DerivedQuantity<ElectricalCharge> {

	public static ElectricalCharge ELEMENTARY_CHARGE = new ElectricalCharge(1.602176620898E-19D);

	private static final long	serialVersionUID	= 1L;
	final static Unit			unit				= new Unit(ElectricalCharge.class,	//
			//
			Current.DIMe1,														//
			Time.DIMe1

														);

	ElectricalCharge(double value) {
		super(value);
	}

	public static ElectricalCharge from(double value) {
		return new ElectricalCharge(value);
	}

	@Override
	protected ElectricalCharge same(double value) {
		return from(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static ElectricalCharge fromAh(double value) {
		return new ElectricalCharge(value * 3600);
	}

	public double toAh() {
		return this.value / 3600;
	}
}
