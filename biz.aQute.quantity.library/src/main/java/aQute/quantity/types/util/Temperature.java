package aQute.quantity.types.util;

import aQute.quantity.base.util.BaseQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

@UnitInfo(unit = "K", symbol="T", dimension = "Temperature", symbolForDimension = "Θ", description="Average energy per degree of freedom of a system")
public class Temperature extends BaseQuantity<Temperature> {
	private static final double	_ZERO_FAHRENHEIT	= 459.67;
	private static final long	serialVersionUID	= 1L;
	private static final double	_ZERO_CELSIUS		= 273.16;
	private static final Unit	unit				= new Unit(Temperature.class);

	public final static Temperature	ZERO_CELSIUS	= fromCelsius(_ZERO_CELSIUS);
	public final static Temperature	HUNDRED_CELSIUS	= fromCelsius(_ZERO_CELSIUS + 100);
	public final static Temperature	ZERO_FAHRENHEIT	= fromFahrenheit(_ZERO_FAHRENHEIT);

	public Temperature(double value) {
		super(value);
	}

	public static Temperature from(double value) {
		return new Temperature(value);
	}

	public double toCelsius() {
		return value - _ZERO_CELSIUS;
	}

	public static Temperature fromCelsius(double value) {
		return Temperature.from(value + _ZERO_CELSIUS);
	}

	// [K] = ([°F] + 459.67) × 5⁄9
	public static Temperature fromFahrenheit(double value) {
		return from((value + 459.67) * 5 / 9);
	}

	// [°F] = [K] × 9⁄5 − 459.67
	public double toFahrenheit() {
		return value * 9 / 5 - _ZERO_FAHRENHEIT;
	}

	@Override
	protected Temperature same(double value) {
		return from(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}
}
