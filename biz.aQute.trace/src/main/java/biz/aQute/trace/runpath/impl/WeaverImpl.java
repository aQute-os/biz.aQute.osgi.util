package biz.aQute.trace.runpath.impl;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.FrameworkWiring;

import aQute.lib.collections.MultiMap;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import biz.aQute.trace.activate.ActivationTracer;
import javassist.CannotCompileException;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;

/**
 * The WeavingHook service weaves the Bundle Activators, components lifecycle
 * methods, and extra methods if so added via
 * {@link ActivationTracer#trace(String)}. A woven method will report events at
 * {@link ActivationTracer#event(Object, String, String, String)}.
 */
class WeaverImpl implements WeavingHook, SynchronousBundleListener {
	final static Pattern			IMPLEMENTATION_P	= Pattern
		.compile("<implementation(\\s|\n|\r)+class\\s*=\\s*\"(?<fqn>.*)\"");
	final FrameworkWiring			frameworkWiring;
	final BundleContext				context;
	final MultiMap<Bundle, String>	classes				= new MultiMap<>();

	WeaverImpl(BundleContext context) {
		this.context = context;
		this.frameworkWiring = context.getBundle(0)
			.adapt(FrameworkWiring.class);
		context.addBundleListener(this);
	}

	void close() {
		context.removeBundleListener(this);
	}

	@Override
	public void weave(WovenClass wovenClass) {

		try {
			Bundle bundle = wovenClass.getBundleWiring()
				.getBundle();

			if (bundle.getBundleId() == 0L)
				return;

			String bundleActivator = bundle.getHeaders()
				.get("Bundle-Activator");

			String[] args = ActivationTracer.extra.get(wovenClass.getClassName());
			if (args != null) {
				doExtra(wovenClass, args[1], args[2]);
				return;
			}

			if (bundleActivator != null) {
				if (bundleActivator.equals(wovenClass.getClassName())) {
					doActivator(wovenClass);
				}
				return;
			}

			String componentHeader = bundle.getHeaders()
				.get("Service-Component");

			if (componentHeader != null) {
				debug("component %s", componentHeader);
				List<String> list = getComponentClasses(bundle, componentHeader);

				if (list != null && list.contains(wovenClass.getClassName())) {
					doComponentClass(bundle, componentHeader, wovenClass);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Clean up any data from bundles
	 */
	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.RESOLVED || event.getType() == BundleEvent.UNINSTALLED) {
			synchronized (this) {
				classes.remove(event.getBundle());
			}
		}
	}

	private void doExtra(WovenClass wovenClass, String method, String action) throws Exception {
		boolean changed = false;
		final ClassPool cp = getPool();

		ByteArrayInputStream bin = new ByteArrayInputStream(wovenClass.getBytes());
		DataInputStream din = new DataInputStream(bin);
		ClassFile cf = new ClassFile(din);
		CtClass c = cp.makeClass(cf);

		for (CtMethod m : c.getMethods()) {
			if (m.getName()
				.equals(method)) {
				weave(wovenClass.getClassName(), m, action);
				save(wovenClass, c);
			}
		}
	}

	private List<String> getComponentClasses(Bundle bundle, String componentHeader) {
		List<String> list;
		synchronized (this) {
			list = classes.get(bundle);
			if (list == null)
				try {
					parseXML(bundle, componentHeader);
					list = classes.get(bundle);
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
		return list;
	}

	private void doActivator(WovenClass wovenClass) throws IOException, CannotCompileException {
		final ClassPool cp = getPool();

		ByteArrayInputStream bin = new ByteArrayInputStream(wovenClass.getBytes());
		DataInputStream din = new DataInputStream(bin);
		ClassFile cf = new ClassFile(din);
		CtClass c = cp.makeClass(cf);

		for (CtMethod m : c.getMethods()) {
			if (m.getName()
				.equals("start")
				&& m.getSignature()
					.equals("(Lorg/osgi/framework/BundleContext;)V")) {
				weave(wovenClass.getClassName(), m, "S");
				save(wovenClass, c);
				break;
			} else if (m.getName()
				.equals("stop")
				&& m.getSignature()
					.equals("(Lorg/osgi/framework/BundleContext;)V")) {
				weave(wovenClass.getClassName(), m, "X");
				save(wovenClass, c);
				break;
			}
		}
	}

	private void doComponentClass(Bundle bundle, String componentHeader, WovenClass wovenClass) throws Exception {
		boolean changed = false;
		final ClassPool cp = getPool();

		ByteArrayInputStream bin = new ByteArrayInputStream(wovenClass.getBytes());
		DataInputStream din = new DataInputStream(bin);
		ClassFile cf = new ClassFile(din);
		CtClass c = cp.makeClass(cf);

		for (CtMethod m : c.getMethods()) {
			if (m.hasAnnotation("org.osgi.service.component.annotations.Activate")) {
				weave(wovenClass.getClassName(), m, "A");
				changed = true;
			} else if (m.hasAnnotation("org.osgi.service.component.annotations.Deactivate")) {
				weave(wovenClass.getClassName(), m, "D");
				changed = true;
			} else if (m.hasAnnotation("org.osgi.service.component.annotations.Modified")) {
				weave(wovenClass.getClassName(), m, "M");
				changed = true;
			}
		}
		for (CtConstructor m : c.getConstructors()) {
			if (m.hasAnnotation("org.osgi.service.component.annotations.Activate")
				&& Modifier.isPublic(c.getModifiers())) {
				weaveCt(wovenClass.getClassName(), m, "C");
				changed = true;
			}
		}

		if (changed) {
			save(wovenClass, c);
		}
	}

	private void save(WovenClass wovenClass, CtClass c) throws IOException, CannotCompileException {
		wovenClass.getDynamicImports()
			.add(ActivationTracer.class.getPackage()
				.getName());
		byte[] bytes = c.toBytecode();
		wovenClass.setBytes(bytes);
	}

	private void parseXML(Bundle bundle, String componentHeader) throws IOException {
		debug("parse %s", componentHeader);
		for (String path : Strings.split(componentHeader)) {
			if (path.contains("*")) {
				int n = path.lastIndexOf("/");
				assert n >= 0;
				String filePattern = path.substring(n + 1);
				path = path.substring(0, n);
				Enumeration<URL> findEntries = bundle.findEntries(path, filePattern, false);
				while (findEntries.hasMoreElements())
					parseXML(bundle, componentHeader, findEntries.nextElement());
			} else {
				URL url = bundle.getEntry(path);
				parseXML(bundle, componentHeader, url);
			}
		}
	}

	private void parseXML(Bundle bundle, String componentHeader, URL url) throws IOException {
		debug("parse %s %s %s", bundle, componentHeader, url);
		if (url != null) {
			String xml = IO.collect(url);
			Matcher matcher = IMPLEMENTATION_P.matcher(xml);
			if (matcher.find()) {
				classes.add(bundle, matcher.group("fqn"));
			}
		} else {
			System.err.println("No xml " + bundle + " " + componentHeader);
		}
	}

	private void weave(String className, CtMethod m, String type) throws CannotCompileException {
		try {
			debug("weave %s %s%s %s", className, m.getName(), m.getSignature(), type);
			m.insertBefore("{ ActivationTracer.event( this,\"" + m.getLongName() + "\",\"" + type + "\", \">\"); }");
			m.insertAfter("{ ActivationTracer.event(this,\"" + m.getLongName() + "\",\"" + type + "\", \"<\"); }", true,
				false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void weaveCt(String className, CtConstructor m, String type) throws CannotCompileException {
		try {
			int n = className.lastIndexOf('.');
			String simpleName = className.substring(n + 1);

			String longName = className + "." + simpleName + "()";
			debug("weave %s %s%s %s", className, longName, m.getSignature(), type);
			m.insertBeforeBody(
				"{ ActivationTracer.event( this,\"" + longName + "\",\"" + type + "\", \">\"); }");
			m.insertAfter("{ ActivationTracer.event(this,\"" + longName + "\",\"" + type + "\", \"<\"); }", true,
				false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ClassPool getPool() {
		final ClassPool cp = new ClassPool();
		cp.appendClassPath(new ClassPath() {

			@Override
			public InputStream openClassfile(String arg0) throws NotFoundException {
				URL find = find(arg0);
				if (find != null) {
					try {
						return find.openStream();
					} catch (IOException e) {
						throw new NotFoundException(e.getMessage());
					}
				}
				return null;
			}

			@Override
			public URL find(String fqn) {
				String path = fqn.replace('.', '/') + ".class";
				for (Bundle b : context.getBundles()) {
					URL entry = b.getEntry(path);
					if (entry != null)
						return entry;
				}

				return WeaverImpl.class.getClassLoader()
					.getResource(path);
			}
		});
		cp.appendSystemPath();
		cp.importPackage(ActivationTracer.class.getPackage()
			.getName());
		return cp;
	}

	private void debug(String format, Object... args) {
		if (ActivationTracer.debug)
			try {
				System.out.printf(format + "%n", args);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
}
