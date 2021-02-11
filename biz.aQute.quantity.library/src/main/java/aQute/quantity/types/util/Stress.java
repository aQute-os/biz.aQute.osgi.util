package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit="Pa", symbol="σ", dimension="Stress", symbolForDimension="p")
public class Stress extends DerivedQuantity<Stress>{
	private static final long serialVersionUID = 1L;
	private static final Unit unit = new Unit(Stress.class, Mass.DIMe1, Length.DIMe1, Time.DIMe_2);

	public Stress(double value) {
		super(value);
	}

	@Override
	protected Stress same(double value) {
		return from(value);
	}

	public static Stress from(double value) {
		return new Stress(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static Stress fromAtmosphere( double value ) {
		return from(value * 101325d);
	}
	
	public double toAtmosphere() {
		return value / 101325d;
	}

	public static Stress fromBar( double value ) {
		return from(value * 1000d);
	}
	
	public double toBar() {
		return value / 1000d;
	}
	
	public static Stress fromPoundPerSquareInch( double psi) {
		return Stress.from(psi * 6894.76d);
	}
	
	public  double toPoundPerSquareInch() {
		return value / 6894.76d;
	}

	public static Stress fromTorr( double torr) {
		return Stress.from(torr * 33.32242079569d);
	}
	
	public  double toTorr() {
		return value / 33.32242079569d;
	}

	public static Stress fromHPa( double HPa) {
		return Stress.from( HPa * 100);
	}
	
	public  double toHPa() {
		return value / 100;
	}
}
