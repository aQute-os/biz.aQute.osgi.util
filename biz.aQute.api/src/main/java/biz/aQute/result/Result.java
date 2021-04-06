package biz.aQute.result;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Result<T> {

	@Override
	public String toString() {
		return message == null ? "ok["+value+"]" : "err["+message+"]";
	}

	final T			value;
	final String	message;

	public static <X> Result<X> ok(X value) {
		return new Result<X>(value);
	}

	public static <X> Result<X> error(String format, Object... args) {
		try {
			for (int i = 0; i < args.length; i++) {
				args[i] = print(args[i], new HashSet<>());
			}
			return new Result<X>(String.format(format, args));
		} catch (Exception e) {
			return new Result<X>(format + ": " + args);
		}
	}

	public T unwrap() {
		return value;
	}

	public Optional<T> get() {
		if (message != null)
			return Optional.empty();
		else
			return Optional.of(value);
	}

	public boolean isOk() {
		return message == null;
	}

	public boolean isErr() {
		return !isOk();
	}

	public String getMessage() {
		return message;
	}

	static Object print(Object object, Set<Object> visited) {
		if (object == null)
			return null;

		if (!visited.add(object))
			return "cycle : " + object;

		if (object.getClass()
			.isArray()) {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			String del = "";
			for (int i = 0; i < Array.getLength(object); i++) {
				if (sb.length() > 1000) {
					sb.append(" ...");
					return sb.toString();
				}
				sb.append(del)
					.append(print(Array.get(object, i), visited));
				del = ",";
				if (sb.length() > 1000) {
					sb.append(" ...");
					return sb.toString();
				}
			}
			sb.append("]");
			return sb;
		}
		return object;
	}

	Result(T value) {
		this.value = value;
		this.message = null;
	}

	Result(String message) {
		this.value = null;
		this.message = message;
	}
}
