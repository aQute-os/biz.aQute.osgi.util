package biz.aQute.modbus.reflective;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import aQute.lib.converter.Converter;
import aQute.lib.strings.Strings;
import biz.aQute.result.Result;

public class Reflector {

	static class SetGet<T> {
		final Consumer<T> set;
		final Supplier<T> get;

		SetGet(Consumer<T> set, Supplier<T> get) {
			this.set = set;
			this.get = get;
		}
	}

	private Object[] targets;
	private Map<String,SetGet<?>>	info = new HashMap<>();

	public Reflector(Object[] targets) {
		this.targets = targets;
	}


	public <T> Result<T> set( String name, T value) {
		@SuppressWarnings("unchecked")
		SetGet<T> sg = (SetGet<T>) info.computeIfAbsent(name, this::find);
		if( sg == null)
			return Result.error("no such data %s", name);

		if ( sg.set == null)
			return Result.error("no setter for %s", name);

		sg.set.accept(value);
		return Result.ok(value);
	}

	public <T> Result<T> get( String name) {
		@SuppressWarnings("unchecked")
		SetGet<T> sg = (SetGet<T>) info.computeIfAbsent(name, this::find);
		if( sg == null)
			return Result.error("no such data %s", name);

		if ( sg.get == null)
			return Result.error("no getter for %s", name);

		return Result.ok(sg.get.get());
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> SetGet<?> find(String name) {
		return new SetGet(set(targets,name), get(targets,name));
	}

	private static Supplier<Object> get(Object[] targets, String name) {
		for (Object target : targets) {
			for (Field field : target.getClass().getFields()) {
				if (field.getName().equals(name))
					return () -> {
						try {
							return field.get(target);
						} catch (IllegalArgumentException | IllegalAccessException e) {
							throw new RuntimeException(e);
						}
					};
			}

			String getters[] = toGetters(name);
			for (Method method : target.getClass().getMethods()) {
				if (method.getDeclaringClass() == Object.class)
					continue;

				if (method.getParameterTypes().length != 0)
					continue;

				if (method.getReturnType() == void.class)
					continue;

				if (Strings.in(getters, method.getName()) && method.getParameters().length == 0)
					return () -> {
						try {
							method.setAccessible(true);
							return method.invoke(target);
						} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
							throw new RuntimeException(e);
						}
					};
			}
		}

		return null;
	}

	private static Consumer<Object> set(Object[] targets, String name) {
		for (Object target : targets) {
			for (Field field : target.getClass().getFields()) {
				if (field.getName().equals(name))
					return (v) -> {
						try {
							field.set(target, Converter.cnv(field.getGenericType(), v));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					};
			}
			String setters[] = toSetters(name);
			for (Method method : target.getClass().getMethods()) {
				if (Strings.in(setters, method.getName()) && method.getParameters().length == 0)
					return (v) -> {
						try {
							method.invoke(target, Converter.cnv(method.getGenericReturnType(), v));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					};
			}
		}

		return null;
	}


	private static String[] toGetters(String name) {
		String uppercased = name.substring(0, 1).toUpperCase() + name.substring(1);
		return new String[] {
			name, "get" + uppercased, "is" + uppercased
		};
	}

	private static String[] toSetters(String name) {
		String uppercased = name.substring(0, 1).toUpperCase() + name.substring(1);
		return new String[] {
			name, "set" + uppercased
		};
	}

}
