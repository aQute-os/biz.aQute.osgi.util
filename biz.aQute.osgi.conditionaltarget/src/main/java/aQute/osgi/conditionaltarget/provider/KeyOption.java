package aQute.osgi.conditionaltarget.provider;

import java.util.List;
import java.util.stream.DoubleStream;

/*
 * define aggregate properties. Each enum value specifies a key prefix and defines the aggregate function
 */
enum KeyOption {
	/*
	 * Count the number of the properties with this key after the prefix is removed
	 */
	COUNT("#") {
		@Override
		public Object value(List<? extends Object> l) {
			return l.size();
		}
	},
	/*
	 * Get the maximum value or NaN if no values
	 */
	MAX("[max]") {

		@Override
		Object value(List<? extends Object> l) {
			return toDoubles(l).max().orElse(Double.NaN);
		}
	},
	/*
	 * Get the minimum value or NaN if no values
	 */
	MIN("[min]") {
		@Override
		Object value(List<? extends Object> l) {
			return toDoubles(l).min().orElse(Double.NaN);
		}
	},
	/*
	 * Get the sum of the values
	 */
	SUM("[sum]") {
		@Override
		Object value(List<? extends Object> l) {
			return toDoubles(l).sum();
		}
	},
	/*
	 * Get the average of the values
	 */
	AVG("[avg]") {
		@Override
		Object value(List<? extends Object> l) {
			return toDoubles(l).average().orElse(Double.NaN);
		}
	},
	/*
	 * Count the unique values
	 */
	UNQ("[unq]") {
		@Override
		Object value(List<? extends Object> l) {
			return l.stream().distinct().count();
		}
	};
	private static DoubleStream toDoubles(List<? extends Object> l) {
		return l.stream().filter(Number.class::isInstance).map(Number.class::cast).mapToDouble(Number::doubleValue);
	}

	String symbol;

	abstract Object value(List<? extends Object> o);

	KeyOption(String s) {
		this.symbol = s;

	}
}