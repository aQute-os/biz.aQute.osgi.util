package aQute.jpm.api;

import java.util.List;
import java.util.Map;

import aQute.struct.Define;
import aQute.struct.Patterns;
import aQute.struct.struct;

public class CommandData extends struct {

	/**
	 * Time of last update
	 */
	public long					time			= System.currentTimeMillis();

	/**
	 * The coordinate used to install this. Could be a URL, file, SHA, or GAV.
	 * It should provide you with a File reference in the
	 * {@link JPM#getArtifact(String)} method.
	 */
	public String				coordinate;

	/**
	 * The name of the command. This must match the file name in the bin dir.
	 */
	@Define(pattern = Patterns.SIMPLE_NAME_S)
	public String				name;

	/**
	 * The title, generally from the JPM-Name in the manifest
	 */
	@Define(optional = true)
	public String				title;

	/**
	 * The descriptions, generally from the Bundle-Description in the manifest
	 */
	@Define(optional = true)
	public String				description;

	/**
	 * The JVM arguments to add
	 */
	@Define(optional = true)
	public String				jvmArgs;

	/**
	 * A list of dependencies. This would include the transitive runtime
	 * dependencies from maven.
	 */
	public List<String>			dependencies	= list();

	/**
	 * The path to the executable file
	 */
	@Define(optional = true)
	public String				bin;
	/**
	 * The executable file for the java vm as set by the user
	 */
	@Define(optional = true)
	public String				java;

	/**
	 * The main fully qualified class name
	 */
	public String				main;
	/**
	 * Use javaw instead of java
	 */
	public boolean				windows;

	/**
	 * Specifies a version range for the vm in the OSGi style. The range must
	 * match the classic 1.6 ... 1.17 kind of versions. (I.e. Java 17 == 1.17).
	 * If you only need a base version, set e.g. '1.12', this is a valid range
	 * from 1.12...infinite. If no range is set, the latest JVM is used.
	 */
	@Define(optional = true, pattern = Patterns.VERSION_RANGE_S)
	public String				range;

	/**
	 * Optional path to VM. This is normally calculated from the VM but this can
	 * be overridden.
	 */
	@Define(optional = true)
	public String				jvm;

	/**
	 * Command line properties
	 */
	public Map<String, String>	properties;
}
