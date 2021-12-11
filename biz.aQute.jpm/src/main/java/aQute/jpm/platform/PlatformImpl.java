package aQute.jpm.platform;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.version.MavenVersion;
import aQute.jpm.api.JVM;
import aQute.jpm.api.Platform;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;

public abstract class PlatformImpl implements Platform {
	final static Logger	logger	= LoggerFactory.getLogger(Windows.class);

	static PlatformImpl	platform;
	static Runtime		runtime	= Runtime.getRuntime();
	Reporter			reporter;
	final File			cache;

	PlatformImpl(File cache) {
		this.cache = cache;
	}

	/**
	 * Get the current platform manager.
	 *
	 * @param reporter
	 * @param type
	 */
	public static PlatformImpl getPlatform(Reporter reporter, Type type, File cache) {
		if (platform == null) {
			if (type == null)
				type = getPlatformType();

			switch (type) {
				case LINUX :
					platform = new Linux(cache);
					break;
				case MACOS :
					platform = new MacOS(cache);
					break;
				case WINDOWS :
					platform = new Windows(cache);
					break;
				default :
					return null;
			}
		}
		platform.reporter = reporter;
		return platform;
	}

	public static Type getPlatformType() {
		String osName = System.getProperty("os.name")
			.toLowerCase();
		if (osName.startsWith("windows"))
			return Type.WINDOWS;
		else if (osName.startsWith("mac") || osName.startsWith("darwin")) {
			return Type.MACOS;
		} else if (osName.contains("linux"))
			return Type.LINUX;

		return Type.UNKNOWN;
	}

	@Override
	public String toString() {
		return getName();
	}

	/**
	 * <pre>
	 * IMPLEMENTOR="Azul Systems, Inc."
	IMPLEMENTOR_VERSION="Zulu17.30+15-CA"
	JAVA_VERSION="17.0.1"
	JAVA_VERSION_DATE="2021-10-19"
	LIBC="default"
	MODULES="java.base java.compiler java.datatransfer java.xml java.prefs java.desktop java.instrument java.logging java.management java.security.sasl java.naming java.rmi java.management.rmi java.net.http java.scripting java.security.jgss java.transaction.xa java.sql java.sql.rowset java.xml.crypto java.se java.smartcardio jdk.accessibility jdk.internal.jvmstat jdk.attach jdk.charsets jdk.compiler jdk.crypto.ec jdk.crypto.cryptoki jdk.crypto.mscapi jdk.dynalink jdk.internal.ed jdk.editpad jdk.hotspot.agent jdk.httpserver jdk.incubator.foreign jdk.incubator.vector jdk.internal.le jdk.internal.opt jdk.internal.vm.ci jdk.internal.vm.compiler jdk.internal.vm.compiler.management jdk.jartool jdk.javadoc jdk.jcmd jdk.management jdk.management.agent jdk.jconsole jdk.jdeps jdk.jdwp.agent jdk.jdi jdk.jfr jdk.jlink jdk.jpackage jdk.jshell jdk.jsobject jdk.jstatd jdk.localedata jdk.management.jfr jdk.naming.dns jdk.naming.rmi jdk.net jdk.nio.mapmode jdk.random jdk.sctp jdk.security.auth jdk.security.jgss jdk.unsupported jdk.unsupported.desktop jdk.xml.dom jdk.zipfs"
	OS_ARCH="x86_64"
	OS_NAME="Windows"
	SOURCE=".:git:f6b830aa983b"
	 * </pre>
	 */

	public JVM getJVM(File vmdir) throws Exception {
		if (!vmdir.isDirectory()) {
			return null;
		}

		File binDir = new File(vmdir, "bin");
		if (!binDir.isDirectory()) {
			logger.debug("Found a directory {}, but it does not have the expected bin directory", vmdir);
			return null;
		}

		File releaseFile = new File(vmdir, "release");
		if (!releaseFile.isFile() || !releaseFile.exists()) {
			logger.debug("Found a directory {}, but it doesn't contain an expected release file", vmdir);
			return null;
		}

		try (InputStream is = IO.stream(releaseFile)) {
			Properties releaseProps = new Properties();
			releaseProps.load(is);

			JVM jvm = new JVM();
			jvm.name = vmdir.getName();
			jvm.vendor = cleanup(releaseProps.getProperty("IMPLEMENTOR"));
			jvm.javahome = vmdir.getCanonicalPath();
			jvm.version = cleanup(releaseProps.getProperty("JAVA_VERSION"));
			jvm.platformVersion = MavenVersion.cleanupVersion(jvm.version);
			jvm.modules = cleanup(releaseProps.getProperty("MODULES"));
			jvm.os_arch = cleanup(releaseProps.getProperty("OS_ARCH"));
			jvm.os_name = cleanup(releaseProps.getProperty("OS_NAME"));

			return jvm;
		}
	}

	String cleanup(String v) {
		if (v == null)
			return "";

		v = v.trim();
		if (v.startsWith("\""))
			v = v.substring(1);
		if (v.endsWith("\""))
			v = v.substring(0, v.length() - 1);

		return v;
	}

	String priority(String... options) {
		for (String option : options) {
			if (option != null)
				return option;
		}
		return null;
	}

}
