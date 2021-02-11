package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit="V", symbol="U", dimension="Voltage", symbolForDimension="")
public class ElectricPotential extends DerivedQuantity<ElectricPotential>{
	private static final long serialVersionUID = 1;
	private static final Unit dimension = new Unit(ElectricPotential.class, Mass.DIMe1, Length.DIMe2, Current.DIMe_1, Time.DIMe_3 );

	ElectricPotential(double value) {
		super(value);
	}

	@Override
	protected ElectricPotential same(double value) {
		return from(value);
	}

	public static ElectricPotential from(double value) {
		return new ElectricPotential(value);
	}

	@Override
	public Unit getUnit() {
		return dimension;
	}
	
	public Power power( Current ampere) {
		return Power.from(value * ampere.value);
	}

	

}
