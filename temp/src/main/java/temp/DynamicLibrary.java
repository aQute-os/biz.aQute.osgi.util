package temp;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class DynamicLibrary<T extends Library> implements Closeable {
	final static Method	findLibrary;
	final T				proxy;
	final Class<T>		type;
	final String		path;
	final NativeLibrary	lib;
	final Closeable		deps;

	static {
		try {
			ClassLoader b = NativeCodeTest.class.getClassLoader();
			findLibrary = b.getClass().getDeclaredMethod("findLibrary", String.class);
			findLibrary.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	public static Closeable dependencies(ClassLoader loader, String... libs) {
		List<DynamicLibrary<?>> deps = new ArrayList<>();
		for (String lib : libs) {
			boolean mandatory=true;
			String actual;
			if (lib.startsWith("-")) {
				actual = lib.substring(1);
				mandatory = false;
			}
			else
				actual = lib;

			try (DynamicLibrary<Library> dynlib = new DynamicLibrary<>(actual, loader)) {
				if (dynlib.isLoaded())
					deps.add(dynlib);
				else if (mandatory)
					throw new IllegalArgumentException(
							"Dependency mandatory (does not start with -) but could not find it: " + lib
									+ " osname=" + System.getProperty("os.name")
									+ " processor=" + System.getProperty("os.arch"));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return () -> {
			Collections.reverse(deps);
			for (DynamicLibrary<?> dynlib : deps) {
				dynlib.close();
			}
		};
	}

	public DynamicLibrary(String name, Class<T> type, String... libs) {
		try {
			this.deps = dependencies(type.getClassLoader(), libs);
			this.type = type;
			this.path = (String) findLibrary.invoke(type.getClassLoader(), name);
			if (path == null)
				throw new IllegalStateException("no such library found " + name + " for " + type);

			this.lib = NativeLibrary.getInstance(this.path);
			this.proxy = Native.load(this.path, type);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public DynamicLibrary(String name, ClassLoader loader) {
		try {
			this.deps = null;
			this.path = (String) findLibrary.invoke(loader, name);
			if (path == null) {
				this.lib = null;
			} else {

				this.lib = NativeLibrary.getInstance(this.path);
			}
			this.proxy = null;
			this.type = null;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isLoaded() {
		return lib != null;
	}

	@Override
	public void close() throws IOException {
		if (isLoaded())
			lib.dispose();
		if (deps != null)
			deps.close();
	}

	public Optional<T> get() {
		return Optional.ofNullable(proxy);
	}
}
