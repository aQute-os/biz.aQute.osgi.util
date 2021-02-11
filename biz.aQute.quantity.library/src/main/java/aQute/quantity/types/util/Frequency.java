package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit="Hz", symbol="f", dimension="Frequency", symbolForDimension="")
public class Frequency extends DerivedQuantity<Frequency> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	dimension			= new Unit(Frequency.class, Time.DIMe_1);

	public Frequency(double value) {
		super(value);
	}

	@Override
	protected Frequency same(double value) {
		return from(value);
	}

	public static Frequency from(double value) {
		return new Frequency(value);
	}

	@Override
	public Unit getUnit() {
		return dimension;
	}

	public static Frequency fromKilohertz(double v) {
		return from(v * 1000);
	}

	public double toKilohertz() {
		return value / 1000;
	}

	public static Frequency fromMegahertz(double v) {
		return from(v * 1_000_000);
	}

	public double toMegahertz() {
		return value / 1_000_000;
	}

	public static Frequency fromGigahertz(double v) {
		return from(v * 1_000_000_000);
	}

	public double toGigahertz() {
		return value / 1_000_000_000;
	}

	public Length wavelength(Velocity v) {
		return Length.from(v.value / value);
	}

	public Length 𝛌(Velocity v) {
		return Length.from(v.value / value);
	}

	public Velocity velocity(Length wavelength) {
		return Velocity.from(wavelength.value * value);
	}

	public Time inverse() {
		return Time.from( 1 / value );
	}
}
