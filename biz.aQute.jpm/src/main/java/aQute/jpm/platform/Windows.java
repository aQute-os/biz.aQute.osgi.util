package aQute.jpm.platform;

/**
 * http://support.microsoft.com/kb/814596
 */
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.jpm.api.CommandData;
import aQute.jpm.api.JVM;
import aQute.lib.io.IO;
import aQute.libg.command.Command;

/**
 * The Windows platform uses an open source library
 * <a href="http://winrun4j.sourceforge.net/">WinRun4j</a> or
 * <a href="https://github.com/poidasmith/winrun4j">on github</a>. An executable
 * is copied to the path of the desired command. When this command is executed,
 * it looks up the same path, but then with the .exe replaced with .ini. This
 * ini file then describes what Java code to start. For JPM, we copy the base
 * exe (either console and/or 64 bit arch) and then create the ini file from the
 * jpm command data.
 * <p>
 * TODO services (fortunately, winrun4j has extensive support)
 */
class Windows extends PlatformImpl {
	final static Pattern	JAVA_HOME	= Pattern.compile("JavaHome\\s+REG_SZ\\s+(?<path>[^\n\r]+)");
	final static Logger		logger		= LoggerFactory.getLogger(Windows.class);
	final static boolean	IS64		= System.getProperty("os.arch")
			.contains("64");

	final File				misc;

	Windows(File cache) {
		super(cache);
		misc = new File(cache, "misc");
	}

	@Override
	public String getName() {
		return "Windows";
	}

	/**
	 * The uninstaller should be used
	 */
	@Override
	public void uninstall() throws IOException {
	}

	/**
	 * Create a new command. Firgure out if we need the console or the window
	 * version and the 64 or 32 bit version of the exe. Copy it, and create the
	 * ini file.
	 */
	@Override
	public String createCommand(CommandData data, Map<String, String> map, boolean force, JVM jvm, File binDir,
			String... extra) throws Exception {
		if (map == null)
			map = Collections.emptyMap();

		//
		// The path to the executable
		//
		data.bin = new File(binDir, data.name + ".exe").getAbsolutePath();
		File f = new File(data.bin);

		if (!force && f.exists())
			return "Command already exists " + data.bin + ", try to use --force";

		//
		// Pick console or windows (java/javaw)
		//
		if (data.windows)
			IO.copy(new File(misc, "winrun4j.exe"), f);
		else
			IO.copy(new File(misc, "winrun4jc.exe"), f);

		//
		// Make the ini file
		//

		File ini = new File(f.getAbsolutePath()
				.replaceAll("\\.exe$", ".ini"));
		Charset defaultCharset = Charset.defaultCharset();
		try (PrintWriter pw = new PrintWriter(ini, defaultCharset.name())) {
			pw.printf("main.class=%s%n", data.main);

			//
			// Add all the calculated dependencies
			//
			int n = 1;
			for (String spec : data.dependencies) {
				pw.printf("classpath.%d=%s%n", n++, spec);
			}

			pw.printf("%n");

			//
			// And the vm arguments.
			//
			if (data.jvmArgs != null && data.jvmArgs.length() != 0) {
				String parts[] = data.jvmArgs.split("\\s+");
				for (int i = 0; i < parts.length; i++)
					pw.printf("vmarg.%d=%s%n", i + 1, parts[i]);
			}
			
			if (data.java != null) {
				pw.printf("vm.location=%s%n", data.java);
			} else {
				File javahome =new File(jvm.javahome);
				
				File jvmdll =  IO.getFile(javahome, "jre/bin/server/jvm.dll");
				if ( !jvmdll.isFile())
					throw new IllegalArgumentException("no jvm.dll found in " + jvm.javahome);
				logger.debug("found jvm.dll  {}",jvmdll);
				pw.printf("vm.location=%s%n", jvmdll.getAbsolutePath());
			}
			Map<String, String> map2 = new HashMap<>(map);
			map2.putIfAbsent("log.level", "error");
			for (Map.Entry<String, String> e : map2.entrySet()) {
				pw.printf("%s=%s%n", e.getKey(), e.getValue());
			}

		}
		logger.debug("Ini content {}", IO.collect(ini, defaultCharset));
		return null;
	}

	@Override
	public void deleteCommand(CommandData cmd) throws Exception {
		String executable = cmd.bin;
		File f = new File(executable);
		File fj = new File(f.getAbsolutePath()
				.replaceAll("\\.exe$", ".ini"));
		if (cmd.name.equals("jpm")) {
			logger.debug("leaving jpm behind");
			return;
		} else {
			IO.delete(f);
			IO.delete(fj);
		}
	}

	@Override
	public String toString() {
		try {
			return "Windows x64=" + IS64;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Provide as much detail about the jpm environment as possible.
	 */

	@Override
	public void report(Formatter f) {
	}

	/**
	 * Initialize the directories for windows.
	 *
	 * @throws Exception
	 */

	@Override
	public void init() throws Exception {
		super.init();
		IO.mkdirs(misc);
		if (IS64) {
			logger.debug("copying 64");
			IO.copy(getClass().getResourceAsStream("windows/winrun4jc64.exe"), new File(misc, "winrun4jc.exe"));
			IO.copy(getClass().getResourceAsStream("windows/winrun4j64.exe"), new File(misc, "winrun4j.exe"));
		} else {
			logger.debug("copying 32");
			IO.copy(getClass().getResourceAsStream("windows/winrun4j.exe"), new File(misc, "winrun4j.exe"));
			IO.copy(getClass().getResourceAsStream("windows/winrun4jc.exe"), new File(misc, "winrun4jc.exe"));
		}
	}

	@Override
	public boolean hasPost() {
		return true;
	}

	@Override
	public void doPostInstall() {
		System.out.println("In post install");
	}

	@Override
	public void getVMs(Collection<JVM> vms) throws Exception {
		Command c = new Command();
		c.add("REG");
		c.add("QUERY");
		c.add("HKEY_LOCAL_MACHINE\\SOFTWARE\\JavaSoft");
		c.add("/s");
		StringBuilder sb = new StringBuilder();
		c.execute(sb, sb);

		Matcher m = JAVA_HOME.matcher(sb);
		Set<String> homes = new TreeSet<>();
		while (m.find()) {
			homes.add(m.group("path"));
		}

		for (String p : homes) {
			File javahome = new File(p);
			JVM vm = getJVM(javahome);
			if (vm != null) {
				vms.add(vm);
			}
		}

		String jhome = System.getenv().get("JAVA_HOME");
		if (jhome != null) {
			File j = new File(jhome);

			JVM jvm = getJVM(j);
			if (jvm != null) {
				vms.add(jvm);
			}
		}
	}

}
