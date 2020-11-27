package biz.aQute.osgi.jna.support.provider;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

import com.sun.jna.Library;
import com.sun.jna.Library.Handler;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import aQute.libg.parameters.ParameterMap;

/**
 * This class supports JNA library loading on OSGi. JNA works very well in a
 * static classic class path situation but unfortunately, it uses garbage
 * collection to free up the dynamic libraries.
 * <p>
 * This code wraps a JNA native library for OSGi. First, it will use the OSGi
 * Bundle-NativeCode header to properly use libraries that differ per Bundle
 * Revision. Normally jna is used with the 'simple' name of a library, for
 * example 'hello'. However, this simple name gets bound with the first native
 * library it finds. Subsequent bundles that use the same simple name, get bound
 * to older libraries from previous revisions. However, the simple name is
 * necessary because it maps differently on different platforms. For example, on
 * Windows it maps to `hello.dll`, on Mac to `libhello.dylib`, and on Linux to
 * `libhello.so`.
 * <p>
 * Therefore, this code will reflectively find out the native library from this
 * simple name. ClassLoaders have a protected method: `findLibrary`. This call
 * returns null of no library is found and otherwise extracts it. This leverages
 * the logic of the Bundle-NativeCode support in OSGi.
 * <p>
 * If found, JNA is asked to load that whole path, not just the simple name.
 * This means that the next bundle revision will ask for a different path since
 * the bundle revision must be somehow encoded in the library path. This
 * prevents any confusion.
 * <p>
 * Sometimes dynamic libraries have dependencies. Since OSGi does not handle
 * this, you need to load these libraries _before_ you load your primary
 * library. These libraries can be quite different on different platforms. This
 * class simplifies this by making it possible to provide a list of
 * dependencies.
 * <p>
 * If some of these libraries in the list are not mandatory on all platforms,
 * then it is possible to prefix them with a `-`. In that case its absence does
 * not generate an error.
 * <p>
 * Calls to the native library locked to ensure that the library is not closed
 * while a call is active. This is of course deadlock prone if the native
 * library blocks on a call to itself on another thread. However, native code
 * supports in Java sucks quite big and imho you should keep it simple. (If this
 * turns out to be a problem, this can become optional.)
 *
 * @param <T>
 */

public class DynamicLibrary<T extends Library> implements Closeable {
	final static Random	random	= new Random();
	final static Method	findLibrary;
	final T				proxy;
	final Class<T>		type;
	final String		path;
	final NativeLibrary	lib;
	final Closeable		deps;
	final Bundle		bundle;
	T					facade;

	final Object		lock	= new Object();

	/*
	 * The findLibrary is protected, so sadly we need to set setAccessible
	 */
	static {
		try {
			findLibrary = ClassLoader.class.getDeclaredMethod("findLibrary", String.class);
			findLibrary.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Load a set of dependent libraries. If the name of the lib starts with a
	 * '-' sign, then no error will be reported if the library cannot be found
	 * for the platform. This can be used to load libraries that code depends on
	 * but that have different names on different platforms, or are not needed
	 * on some platforms.
	 *
	 * @param loader the class loader to use to find the libraries.
	 * @param libs The simple names of the libraries, if starts with '-', no
	 *            error is reported when not found.
	 * @return A closeable to close the libraries.
	 */
	public static Closeable dependencies(ClassLoader loader, String... libs) {
		List<DynamicLibrary<?>> deps = new ArrayList<>();
		for (String lib : libs) {
			boolean mandatory = true;
			String actual;
			if (lib.startsWith("-")) {
				actual = lib.substring(1);
				mandatory = false;
			} else
				actual = lib;

			try (DynamicLibrary<Library> dynlib = new DynamicLibrary<>(actual, loader)) {
				if (dynlib.isLoaded())
					deps.add(dynlib);
				else if (mandatory)
					throw new IllegalArgumentException(
							"Dependency mandatory (does not start with -) but could not find it. ");
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

	/**
	 * Create a dynamic library. This will use the OSGi Framework to find the
	 * path to the native library for the given platform, taking the
	 * Bundle-NativeCode header into account. The native library is then loaded
	 * by its path, ensuring that caching cannot interfere. If JNA is given the
	 * simple name, it will cache it for all bundle revisions. Not good.
	 *
	 * @param simpleName The simple name like 'hello' for libhello.so,
	 *            hello.dll, etc.
	 * @param type The interface type that represents this native code for JNA
	 * @param libs A set of simple lib names this code depends upon
	 */
	@SuppressWarnings("unchecked")
	public DynamicLibrary(String simpleName, Class<T> type, String... libs) {
		try {
			this.bundle = FrameworkUtil.getBundle(type);
			this.deps = dependencies(type.getClassLoader(), libs);
			this.type = type;

			String path = (String) findLibrary.invoke(type.getClassLoader(), simpleName);
			if (path == null)
				throw new IllegalStateException("no such library found " + simpleName + " for " + type + ": " + this);

			this.path = path;
			this.proxy = Native.loadLibrary(this.path, type);
			this.facade = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, (p, m, a) -> {
				synchronized (lock) {
					if (facade == null)
						throw new IllegalStateException("The library is already closed " + this);
					return m.invoke(proxy, a);
				}
			});
			Library.Handler handler = (Handler) Proxy.getInvocationHandler(this.proxy);
			this.lib = handler.getNativeLibrary();
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}

	}

	DynamicLibrary(String name, ClassLoader loader) {
		try {
			this.bundle = loader instanceof BundleReference ? ((BundleReference) loader).getBundle() : null;
			this.deps = null;
			this.path = (String) findLibrary.invoke(loader, name);
			if (path == null) {
				this.lib = null;
			} else {
				this.lib = NativeLibrary.getInstance(this.path);
			}
			this.proxy = null;
			this.type = null;
			this.facade = null;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isLoaded() {
		return lib != null;
	}

	@Override
	public void close() throws IOException {
		synchronized (lock) {
			if (facade == null)
				return;
			facade = null;
		}

		if (isLoaded())
			lib.dispose();

		if (deps != null)
			deps.close();

		System.gc();
	}

	public Optional<T> get() {
		synchronized (lock) {
			return Optional.ofNullable(facade);
		}
	}

	@Override
	public String toString() {

		try (Formatter sb = new Formatter()) {
			sb.format("Path                           %s\n", this.path);
			sb.format("Lib                            %s\n", this.lib);
			sb.format("os.name                        %s\n", System.getProperty("os.name"));
			sb.format("os.arch                        %s\n", System.getProperty("os.arch"));

			if (this.bundle != null) {
				BundleContext context = bundle.getBundleContext();
				String [] fwconstants = new String[] {Constants.FRAMEWORK_OS_NAME, Constants.FRAMEWORK_OS_VERSION, Constants.FRAMEWORK_LANGUAGE,Constants.FRAMEWORK_PROCESSOR};
				for ( String constant : fwconstants) {
					sb.format("%-30s  %s\n", constant, context.getProperty(constant));
				}
				String bnc = bundle.getHeaders().get(Constants.BUNDLE_NATIVECODE);
				if ( bnc == null) {
					sb.format("No %s header in manifest", Constants.BUNDLE_NATIVECODE);
				} else {
					ParameterMap	nativeHeader = new ParameterMap(bnc);
					nativeHeader.entrySet().forEach( e-> {
						sb.format("    %-28s%s\n", e.getKey(), e.getValue());
					});
				}
			}

			return sb.toString();
		}
	}
}
