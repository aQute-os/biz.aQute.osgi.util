package biz.aQute.aspects.impl;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;

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

		Bundle bundle = wovenClass.getBundleWiring()
			.getBundle();

		if (bundle.getBundleId() == 0L)
			return;

		String bundleActivator = bundle.getHeaders()
			.get("Bundle-Activator");

		try {
			if (bundleActivator != null) {
				if (bundleActivator.equals(wovenClass.getClassName())) {
					doActivator(wovenClass);
				}
			}

			String componentHeader = bundle.getHeaders()
				.get("Service-Component");

			if (componentHeader != null) {
				List<String> list = getComponentClasses(bundle, componentHeader);

				if (list != null && list.contains(wovenClass.getClassName())) {
					doComponentClass(bundle, componentHeader, wovenClass);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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
		boolean changed = false;
		final ClassPool cp = getPool();

		ByteArrayInputStream bin = new ByteArrayInputStream(wovenClass.getBytes());
		DataInputStream din = new DataInputStream(bin);
		ClassFile cf = new ClassFile(din);
		CtClass c = cp.makeClass(cf);

		for (CtMethod m : c.getMethods()) {
			if (m.getName()
				.equals("start")
				&& m.getSignature()
					.equals("(Lorg.osgi.framework.BundleContext;)V")) {
				weave(wovenClass.getClassName(), m, "S");
			} else if (m.getName()
				.equals("stop")
				&& m.getSignature()
					.equals("(Lorg.osgi.framework.BundleContext;)V")) {
				weave(wovenClass.getClassName(), m, "X");
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

		if (changed) {
			wovenClass.getDynamicImports()
				.add(ActivationTracer.class.getPackage()
					.getName());
			byte[] bytes = c.toBytecode();
			wovenClass.setBytes(bytes);
		}
	}

	private void parseXML(Bundle bundle, String componentHeader) throws IOException {
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
			m.insertBefore("{ ActivationTracer.event( this,\"" + m.getLongName() + "\",\"" + type + "\", \">\"); }");
			m.insertAfter("{ ActivationTracer.event(this,\"" + m.getLongName() + "\",\"" + type + "\", \"<\"); }", true,
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

	/**
	 * Clean up any
	 */
	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.RESOLVED || event.getType() == BundleEvent.UNINSTALLED) {
			synchronized (this) {
				classes.remove(event.getBundle());
			}
		}
	}

}
