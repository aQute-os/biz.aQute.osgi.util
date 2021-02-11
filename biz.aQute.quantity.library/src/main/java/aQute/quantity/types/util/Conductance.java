package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

/**
 * The siemens (SI unit symbol: S) is the unit of electric conductance, electric
 * susceptance and electric admittance in the International System of Units
 * (SI). Conductance, susceptance, and admittance are the reciprocals of
 * resistance, reactance, and impedance respectively; hence one siemens is equal
 * to the reciprocal of one ohm, and is also referred to as the mho. The 14th
 * General Conference on Weights and Measures approved the addition of the
 * siemens as a derived unit in 1971.
 * 
 * The unit is named after Ernst Werner von Siemens. As with every SI unit whose
 * name is derived from the proper name of a person, the first letter of its
 * symbol is upper case (S); the lower-case "s" is the symbol for the second.
 * When an SI unit is spelled out in English, it should always begin with a
 * lower-case letter (siemens), except where any word would be capitalized.[1]
 * In English, the same form siemens is used both for the singular and
 * plural.[2]
 */
@UnitInfo(unit = "S", symbol="G", dimension = "Conductance", symbolForDimension = "")
public class Conductance extends DerivedQuantity<Conductance> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Conductance.class, Mass.DIMe_1, Length.DIMe_2,
			Time.DIMe3, Current.DIMe2);

	Conductance(double value) {
		super(value);
	}

	@Override
	protected Conductance same(double value) {
		return from(value);
	}

	public static Conductance from(double value) {
		return new Conductance(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public Resistance resistence() {
		return Resistance.from(1 / value);
	}

	public ElectricPotential potential(Current amp) {
		return ElectricPotential.from(value / amp.value);
	}

	public Current current(ElectricPotential v) {
		return Current.from(v.value * value);
	}

	// ohm = watt / amp2 => watt = ohm/amp2
	public Power power(Current amp) {
		return Power.from(value / (amp.value * amp.value));
	}

	// ohm = volt2 / watt => watt = volt2/ohm
	public Power power(ElectricPotential volt) {
		return Power.from((volt.value * volt.value) / value);
	}

	// ohm = second / farad => farad = second/ohm
	public Capacitance capacitance(Time second) {
		return Capacitance.from(second.value / value);
	}

	// ohm = second / farad => second = farad * ohm
	public Time period(Capacitance farad) {
		return Time.from(farad.value / value);
	}

	public Resistance resistance() {
		return inverse();
	}

	public Resistance inverse() {
		return Resistance.from(1 / value);
	}
}
