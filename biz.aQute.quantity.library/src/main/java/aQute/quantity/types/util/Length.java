package aQute.quantity.types.util;

import aQute.quantity.base.util.BaseQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;
import aQute.quantity.base.util.Unit.Dimension;

@UnitInfo(unit = "m", symbol = "l", dimension = "Length", symbolForDimension = "L", description = "The one-dimensional extent of an object")
public class Length extends BaseQuantity<Length> {

	static final Dimension		DIMe1	= Unit.dimension(Length.class, 1);
	static final Dimension		DIMe2	= Unit.dimension(Length.class, 2);
	public static final Dimension	DIMe3	= Unit.dimension(Length.class, 3);;
	static final Dimension		DIMe_1	= Unit.dimension(Length.class, -1);
	public static Dimension		DIMe_2	= Unit.dimension(Length.class, -2);

	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Length.class);

	Length(double value) {
		super(value);
	}

	public static Length from(double value) {
		return new Length(value);
	}

	/**
	 * 
	 */

	public Area square() {
		return Area.fromMeter2(value * value);
	}

	public Volume cubic() {
		return Volume.from(value * value * value);
	}

	public Volume volume(Area m2) {
		return Volume.from(value * m2.value);
	}

	@Override
	protected Length same(double value) {
		return from(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static Length fromFoot(double feet) {
		return Length.from(feet * 0.3048D);
	}

	public double toFoot() {
		return value / 0.3048D;
	}

	public static Length fromMile(double mile) {
		return Length.from(mile * 1609.34D);
	}

	public double toMile() {
		return value / 1609.34D;
	}

	public static Length fromKilometer(double km) {
		return Length.from(km * 1000D);
	}

	public double toKilometer() {
		return value / 1000D;
	}

	public static Length fromCentimeter(double cm) {
		return Length.from(cm / 100);
	}

	public double toCentimeter() {
		return value * 100;
	}

	public static Length fromMillimeter(double mm) {
		return Length.from(mm / 1000);
	}

	public double toMillimeter() {
		return value * 1000;
	}

	public static Length fromMicrometer(double µm) {
		return Length.from(µm / 1_000_000D);
	}

	public double toMicrometer() {
		return value * 1_000_000D;
	}

	public static Length fromNanometer(double nm) {
		return Length.from(nm / 1_000_000_000D);
	}

	public double toNanometer() {
		return value * 1_000_000_000D;
	}

	public static Length fromPicometer(double pm) {
		return Length.from(pm / 1_000_000_000_000D);
	}

	public double toPicometer() {
		return value * 1_000_000_000_000D;
	}

	public static Length fromYard(double yard) {
		return Length.from(yard * 0.9144);
	}

	public double toYard() {
		return value / 0.9144D;
	}

	public static Length fromInch(double inch) {
		return Length.from(inch * 0.0254D);
	}

	public double toInch() {
		return value / 0.0254D;
	}

	public Length fromNauticalMile(double nm) {
		return Length.from(nm * 1852D);
	}

	public double toNauticalMile() {
		return value / 1852D;
	}

	public Velocity div( Time second) {
		return Velocity.from(value / second.value);
	}
}
