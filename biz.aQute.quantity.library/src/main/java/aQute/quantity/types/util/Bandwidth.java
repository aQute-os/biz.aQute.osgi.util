package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit="Byte/s", symbol="bandwidth", dimension = "Bandwidth", symbolForDimension = "")
public class Bandwidth extends DerivedQuantity<Bandwidth> {
	private static final long serialVersionUID = 1L;
	private static final Unit unit = new Unit(Bandwidth.class, Bytes.DIMe1, Time.DIMe_1);
	
	Bandwidth(double value) {
		super(value);
	}

	@Override
	protected Bandwidth same(double value) {
		return Bandwidth.fromBytesPerSecond(value);
	}

	public static Bandwidth fromBytesPerSecond(double value) {
		return new Bandwidth(value);
	}
	
	public double toBytesPerSecond() {
		return value;
	}

	public static Bandwidth fromBytesPerSecond(int value) {
		return new Bandwidth(value);
	}
	
	public int toBitsPerSecond() {
		return (int) (value * 8);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}
	
}
