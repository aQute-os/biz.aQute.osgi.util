package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit="m/s", symbol="v", dimension="Velocity", symbolForDimension="v")
public class Velocity extends DerivedQuantity<Velocity> {
	private static final long		serialVersionUID	= 1L;
	private static final Unit	unit			= new Unit(Velocity.class, Length.DIMe1, Time.DIMe_1);

	Velocity(double value) {
		super(value);
	}

	@Override
	protected Velocity same(double value) {
		return from(value);
	}

	public static Velocity from(double value) {
		return new Velocity(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static Velocity fromKilometerPerHour( double kmh) {
		return Velocity.from( kmh * 0.277778d); 
	}

	public double toKilometerPerHour() {
		return value / 0.277778d;
	}

	public static Velocity fromMilePerHour( double mileh) {
		return Velocity.from( mileh * 0.447040357632d); 
	}

	public double toMilePerHour() {
		return value / 0.447040357632d;
	}

	public static Velocity fromFootPerSecond( double foots) {
		return Velocity.from(foots * 0.3048d);
	}
	
	public double toFootPerSecond() {
		return value / 0.3048d;
	}
	
	public static Velocity fromKnot( double knot) {
		return Velocity.from(knot * 0.514444d);
	}
	
	public double toKnot() {
		return value / 0.514444d;
	}

}
