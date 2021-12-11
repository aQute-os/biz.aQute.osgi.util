package aQute.jpm.api;

import java.io.Closeable;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import biz.aQute.result.Result;

/**
 * Service to manage a set of commands that can be used in the shell for Linux,
 * MacOS, and Windows.
 */
public interface JPM extends Closeable {
	/**
	 * Comma separated list of release urls to maven repos in the settings
	 */
	String	RELEASE_URLS	= "jpm.config.release";
	/**
	 * Comma separated list of snapshot urls to maven repos in the settings
	 */
	String	SNAPSHOT_URLS	= "jpm.config.snapshot";

	/**
	 * Translate a spec tp a File. A spec can be:
	 * <ul>
	 * <li>URL
	 * <li>File path
	 * <li>SHA
	 * <li>Maven group:artifact[:class[:extension]][:version[-SNAPSHOT]]
	 * </ul>
	 *
	 * @param spec the spec
	 * @return a file or a reason why it could not be mapped
	 */
	Result<File> getArtifact(String spec) throws Exception;

	/**
	 * Get the home directory
	 */
	File getHomeDir();

	/**
	 * Get the bin directory
	 */
	File getBinDir();

	/**
	 * Get a list of command datas
	 *
	 * @return the list of commands
	 */
	List<CommandData> getCommands();

	/**
	 * Get the existing data of one command by name
	 *
	 * @param name name of the command
	 * @return a command data
	 */
	Result<CommandData> getCommand(String name) throws Exception;

	/**
	 * Create a command data block for a given coordinate. This will use the
	 * file obtained from {@link #getArtifact(String)}, analyze it and provide
	 * defaults. The command will _not_ be written do disk yet.
	 *
	 * @param coordinate a coordinate to use
	 * @return a new command data, not saved
	 */
	Result<CommandData> createCommandData(String coordinate) throws Exception;

	/**
	 * Save the command data and make it permanent. If the command already
	 * exists, force should be true. This returns a non-null value in the case
	 * of issues.
	 *
	 * @param data the command data
	 * @param force override an existing command
	 * @return null when ok, a reason if there was a failure
	 */
	String saveCommand(CommandData data, boolean force) throws Exception;

	/**
	 * Delete a command from the system.
	 *
	 * @param name the name of the command
	 */
	void rmCommand(String name) throws Exception;

	/**
	 * Initialize. This will find the jar that this is running from, and install
	 * it under the name "jpm".
	 */
	void init() throws Exception;

	/**
	 * Remove all the commands and then the home dir. This is destructive.
	 */
	void deinit() throws Exception;

	/**
	 * Get the Platform in use
	 *
	 * @return the platform
	 */
	Platform getPlatform() throws Exception;

	/**
	 * Get a sorted list of JVMs. The highest version is the first element in
	 * the list. VMs are found in a platform specific way.
	 *
	 * @return the list of VMs.
	 */
	SortedSet<JVM> getVMs() throws Exception;

	Result<JVM> getVM(File javahome) throws Exception;

	/**
	 * Pick a VM for the given version range. If it is a single version, it is a
	 * minimum version (inclusive). If the range is null, it will use the latest
	 * installed Java version. For a range, the highest matching VM is returned
	 *
	 * @param range the version range like OSGi
	 * @return a JVM or null if no matching VM was found
	 */
	JVM selectVM(String range) throws Exception;

	/**
	 * Post install
	 */
	void doPostInstall() throws Exception;

	/**
	 * Get the available revisions of a program
	 */
	Result<List<String>> getRevisions(String program);

	Map<String, String> getSettings();

	void save();

	Result<List<String>> search(String query, int from, int pages);

}
