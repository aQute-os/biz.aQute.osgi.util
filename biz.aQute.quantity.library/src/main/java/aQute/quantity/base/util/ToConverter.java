package aQute.quantity.base.util;

public interface ToConverter<T extends Quantity<T>> {
	double to(T q) throws Exception;
}
