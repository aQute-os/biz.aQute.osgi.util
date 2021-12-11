package aQute.quantity.types.util;

import aQute.quantity.base.util.BaseQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.Unit.Dimension;
import aQute.quantity.base.util.UnitInfo;

/**
 * The candela (/kænˈdɛlə/ or /kænˈdiːlə/; symbol: cd) is the SI base unit of
 * luminous intensity; that is, luminous power per unit solid angle emitted by a
 * point light source in a particular direction.
 * 
 */
@UnitInfo(unit="cd", symbol="L", dimension = "Luminous Intensity", symbolForDimension = "J")
public class LuminousIntensity extends BaseQuantity<LuminousIntensity> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(LuminousIntensity.class);
	public static final Dimension		DIMe1				= Unit.dimension(LuminousIntensity.class, 1);

	LuminousIntensity(double value) {
		super(value);
	}

	LuminousIntensity() {
		super(0D);
	}

	@Override
	protected LuminousIntensity same(double value) {
		return LuminousIntensity.from(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static LuminousIntensity from(double value) {
		return new LuminousIntensity(value);
	}
}
