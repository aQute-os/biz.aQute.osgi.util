package aQute.quantity.types.util;

import aQute.quantity.base.util.BaseQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;
import aQute.quantity.base.util.Unit.Dimension;

/**
 * Ampere. Unit information Unit system SI base unit Unit of Electric current
 * Symbol A. It is named after André-Marie Ampère (1775–1836), French
 * mathematician and physicist, considered the father of electrodynamics.
 * <p>
 * The ampere is equivalent to one {@link ElectricalCharge} (roughly 6.241×1018 times the
 * elementary charge) per second. Amperes are used to express flow rate of
 * electric charge. For any point experiencing a current, if the number of
 * charged particles passing through it — or the charge on the particles passing
 * through it — is increased, the amperes of current at that point will
 * proportionately increase.
 * 
 */

@UnitInfo(unit = "A", symbol = "I", dimension = "Current", symbolForDimension = "I")
public class Current extends BaseQuantity<Current> {

	private static final long		serialVersionUID	= 1L;
	private static Unit				unit				= new Unit(Current.class);
	final public static Current		ZERO				= new Current(0D);
	final public static Current		ONE					= new Current(0D);
	static final Dimension			DIMe1				= Unit.dimension(Current.class, 1);
	static final Dimension			DIMe_1				= Unit.dimension(Current.class, -1);
	public static final Dimension	DIMe2				= Unit.dimension(Current.class, 2);
	public static final Dimension	DIMe_2				= Unit.dimension(Current.class, -2);

	Current(double value) {
		super(value);
	}

	Current() {
		super(0D);
	}

	public static Current from(double value) {
		if (value == 0D)
			return ZERO;
		if (value == 1D)
			return ONE;

		return new Current(value);
	}

	@Override
	protected Current same(double value) {
		return from(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public ElectricalCharge charge(Time s) {
		return ElectricalCharge.from(value * s.value);
	}

	public Power power(ElectricPotential volt) {
		return Power.from(value * volt.value);
	}

	// A = C/s => s= C/A
	public Time time(ElectricalCharge charge) {
		return Time.from(charge.value / value);
	}

}
