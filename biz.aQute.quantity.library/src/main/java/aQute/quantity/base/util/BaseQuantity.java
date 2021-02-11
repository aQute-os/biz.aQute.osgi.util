package aQute.quantity.base.util;

public abstract class BaseQuantity<T extends BaseQuantity<T>> extends Quantity<T> {
	private static final long serialVersionUID = 1L;

	public BaseQuantity(double value) {
		super(value);
	}
	
}
