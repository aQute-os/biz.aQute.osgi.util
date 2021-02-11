package aQute.quantity.base.util;

public interface FromConverter<T extends Quantity<T>> {
	T from( double value) throws Exception;
}
