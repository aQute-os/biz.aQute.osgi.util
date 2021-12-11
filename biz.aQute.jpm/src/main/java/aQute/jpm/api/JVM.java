package aQute.jpm.api;

import java.io.File;
import java.util.Comparator;

import aQute.struct.struct;

public class JVM extends struct {
	public static Comparator<JVM>	comparator	= (a, b) -> b.version.compareTo(a.version);

	/**
	 * The path to the home directory of the JVM. This must be the directory
	 * where the 'bin' folder is stored with the executables and should
	 * generally contain a 'release' file with the release properties
	 */
	public String					javahome;

	public String					platformVersion;
	public String					version;
	public String					vendor;
	public String					name;
	public String					modules;

	public String					os_arch;

	public String					os_name;

	public File javaw() {
		return new File(bin(), "javaw");
	}

	public File java() {
		return new File(bin(), "java");
	}

	public File bin() {
		return new File(javahome, "bin");
	}
	
	public String toString() {
		return name + " " + javahome;
	}

}
