package aQute.jpm.api;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Formatter;
import java.util.Map;

public interface Platform {
	enum Type {
		UNKNOWN,
		WINDOWS,
		LINUX,
		MACOS
	}

	String getName();

	void uninstall() throws Exception;

	String createCommand(CommandData data, Map<String, String> map, boolean force, JVM vm, File binDir, String... deps)
		throws Exception;

	void deleteCommand(CommandData cmd) throws Exception;

	void getVMs(Collection<JVM> vms) throws Exception;

	default boolean hasPost() {
		return false;
	}

	default void doPostInstall() {}

	/**
	 * Is called to initialize the platform if necessary.
	 *
	 * @throws IOException
	 * @throws Exception
	 */

	default void init() throws Exception {}

	void report(Formatter formatter);
}
