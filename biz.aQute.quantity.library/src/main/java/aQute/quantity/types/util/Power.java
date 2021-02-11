package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Logarithmic;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit = "W", symbol = "P", dimension = "Power", symbolForDimension = "")
public class Power extends DerivedQuantity<Power> implements Logarithmic<Power> {
	private static final long serialVersionUID = 1L;
	private static final Unit unit = new Unit(Power.class, Mass.DIMe1, Length.DIMe2, Time.DIMe_2);

	Power(double value) {
		super(value);
	}

	@Override
	public Power same(double value) {
		return from(value);
	}

	public static Power from(double value) {
		return new Power(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public Current toCurrent(ElectricPotential volt) {
		return Current.from(value / volt.value);
	}

	public ElectricPotential toVolt(Current amp) {
		return ElectricPotential.from(value / amp.value);
	}

	public Energy toJoule(Time s) {
		return Energy.from(value * s.value);
	}

	public Intensity intensity(Area area) {
		return Intensity.from(value / area.value);
	}
}
