package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit="W/m²", symbol="I", dimension="Intensity", symbolForDimension="")
public class Intensity extends DerivedQuantity<Intensity>{
	private static final long serialVersionUID = 1L;
	private static final Unit unit = new Unit(Intensity.class,Mass.DIMe1, Time.DIMe_3);

	Intensity(double value) {
		super(value);
	}

	@Override
	protected Intensity same(double value) {
		return from(value);
	}

	public static Intensity from(double value) {
		return new Intensity(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public Current toCurrent( ElectricPotential volt) {
		return Current.from(value / volt.value);
	}

	public ElectricPotential toVolt( Current amp) {
		return ElectricPotential.from(value / amp.value);
	}
	
	public Energy toJoule( Time s) {
		return Energy.from(value / s.value);
	}
		
	public double todB() {
		return Math.log10(value) * 10;
	}
			
	public double todBm() {
		return Math.log10(value) * 10_000;
	}
}
