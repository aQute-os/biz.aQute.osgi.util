package aQute.quantity.types.util;

import aQute.quantity.base.util.BaseQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit="rad", symbol="θ", dimension="Angle", symbolForDimension="1")
public class Radian extends BaseQuantity<Radian>{
	private static final long serialVersionUID = 1L;
	private static final Unit unit = new Unit(Radian.class);

	Radian(double value) {
		super(value);
	}

	
	@Override
	protected Radian same(double value) {
		return from(value);
	}

	static public Radian from(double value) {
		return new Radian(value);
	}


	@Override
	public Unit getUnit() {
		return unit;
	}
	
	public Radian fomDegree( double degree ) {		
		return Radian.from( Math.toRadians(degree) );
	}
	
	public double toDegree() {
		return Math.toDegrees(value);
	}

	
	public Radian fromGradian( double degree ) {
		return Radian.from( degree * 0.015708d);
	}
	
	public double toGradian() {
		return value / 0.015708d;
	}

	
	public Radian fromMinuteOfArc( double minuteOfArc ) {
		return Radian.from( minuteOfArc * 0.000290888d);
	}
	
	public double toMinuteOfArc() {
		return value / 0.000290888d;
	}

	public Radian fromSecondOfArc( double minuteOfArc ) {
		return Radian.from( minuteOfArc * 0.0166667d);
	}
	
	public double toSecondOfArc() {
		return value / 0.0166667d;
	}

	
	public Radian fromAnglemil( double anglemil ) {
		return Radian.from( anglemil * 0.000159155d);
	}
	
	public double toAnglemil() {
		return value / 0.000159155d;
	}


	public double sin() {
		return Math.sin(value);
	}

	public double cos() {
		return Math.cos(value);
	}

	public double tan() {
		return Math.tan(value);
	}
	
}
