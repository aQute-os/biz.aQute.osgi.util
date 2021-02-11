package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

/**
 * The ohm (symbol: Ω) is the SI derived unit of electrical resistance, named
 * after German physicist Georg Simon Ohm. Although several empirically derived
 * standard units for expressing electrical resistance were developed in
 * connection with early telegraphy practice, the British Association for the
 * Advancement of Science proposed a unit derived from existing units of mass,
 * length and time and of a convenient size for practical work as early as 1861.
 * The definition of the ohm was revised several times. Today the definition of
 * the ohm is expressed from the quantum Hall effect.
 */
@UnitInfo(unit="Ω", symbol = "R", dimension = "Electrical resistance", symbolForDimension = "")
public class Resistance extends DerivedQuantity<Resistance> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Resistance.class, Mass.DIMe1, Length.DIMe2, Time.DIMe_3,
			Current.DIMe_2);

	Resistance(double value) {
		super(value);
	}

	@Override
	protected Resistance same(double value) {
		return from(value);
	}

	public static Resistance from(double value) {
		return new Resistance(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public ElectricPotential potential(Current amp) {
		return ElectricPotential.from(value * amp.value);
	}

	public Current current(ElectricPotential v) {
		return Current.from(v.value / value);
	}

	public Conductance conductance() {
		return Conductance.from(1 / value);
	}
	
	public Conductance inverse() {
		return conductance();
	}
}
