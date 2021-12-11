package aQute.jpm.platform;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import aQute.jpm.api.JVM;

class MacOS extends Unix {
	private final static Logger		logger	= LoggerFactory.getLogger(MacOS.class);
	static DocumentBuilderFactory	dbf		= DocumentBuilderFactory.newInstance();
	static XPathFactory				xpf		= XPathFactory.newInstance();

	MacOS(File cache) {
		super(cache);
	}

	@Override
	public String getName() {
		return "MacOS";
	}

	@Override
	public void uninstall() throws IOException {}

	@Override
	public String toString() {
		return "MacOS/Darwin";
	}

	/**
	 * Return the VMs on the platform.
	 *
	 * @throws Exception
	 */
	@Override
	public void getVMs(Collection<JVM> vms) throws Exception {
		String paths[] = {
			"/System/Library/Java/JavaVirtualMachines", "/Library/Java/JavaVirtualMachines", System.getenv("JAVA_HOME")
		};
		for (String path : paths) {
			if (path != null) {
				File[] vmFiles = new File(path).listFiles();
				if (vmFiles != null) {
					for (File vmdir : vmFiles) {
						JVM jvm = getJVM(vmdir);
						if (jvm != null)
							vms.add(jvm);
					}
				}
			}
		}
	}

	@Override
	public JVM getJVM(File vmdir) throws Exception {
		if (!vmdir.isDirectory()) {
			return null;
		}

		File contents = new File(vmdir, "Contents");
		if (!contents.isDirectory()) {
			JVM jvm = _getJVMFromRTJar(vmdir);

			if (jvm != null) {
				return jvm;
			}

			return null;
		}
		File home = new File(contents, "Home");
		JVM x = super.getJVM(home);
		if (x != null) {
			x.name = vmdir.getName();
			return x;
		}

		File plist = new File(contents, "Info.plist");
		if (!plist.isFile()) {
			logger.debug("The VM in {} has no Info.plist with the necessary details", vmdir);
			return null;
		}

		DocumentBuilder db = dbf.newDocumentBuilder();
		try {
			Document doc = db.parse(plist);
			XPath xp = xpf.newXPath();
			Node versionNode = (Node) xp.evaluate("//dict/key[text()='JVMVersion']", doc, XPathConstants.NODE);
			Node platformVersionNode = (Node) xp.evaluate("//dict/key[text()='JVMPlatformVersion']", doc,
				XPathConstants.NODE);
			Node vendorNode = (Node) xp.evaluate("//dict/key[text()='JVMVendor']", doc, XPathConstants.NODE);
			@SuppressWarnings("unused")
			Node capabilitiesNode = (Node) xp.evaluate("//dict/key[text()='JVMCapabilities']", doc,
				XPathConstants.NODE);

			JVM jvm = new JVM();
			jvm.name = vmdir.getName();
			jvm.javahome = home.getCanonicalPath();
			jvm.version = getSiblingValue(versionNode);
			jvm.platformVersion = getSiblingValue(platformVersionNode);
			jvm.vendor = getSiblingValue(vendorNode);

			return jvm;
		} catch (Exception e) {
			logger.debug("Could not parse the Info.plist in {}", vmdir, e);
			throw e;
		}
	}

	private JVM _getJVMFromRTJar(File vmdir) throws Exception {
		File rtJar = new File(vmdir, "lib/rt.jar");

		if (rtJar.exists()) {
			try (JarFile jarFile = new JarFile(rtJar)) {
				File vm = vmdir.getCanonicalFile();

				JVM jvm = new JVM();
				jvm.name = vm.getName();
				jvm.javahome = vm.getCanonicalPath();

				Manifest manifest = jarFile.getManifest();
				Attributes attrs = manifest.getMainAttributes();
				jvm.version = attrs.getValue("Specification-Version");
				jvm.platformVersion = attrs.getValue("Implementation-Version");
				jvm.vendor = attrs.getValue("Specification-Vendor");

				return jvm;
			} catch (Exception e) {
				logger.debug("Could not get versions from rt.jar {}", vmdir, e);
				throw e;
			}
		}

		return null;
	}

	private String getSiblingValue(Node node) {
		if (node == null)
			return null;
		node = node.getNextSibling();
		while (node.getNodeType() == Node.TEXT_NODE)
			node = node.getNextSibling();

		return node.getTextContent();
	}

}
