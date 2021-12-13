package aQute.jpm.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.Level;
import org.slf4j.impl.StaticLoggerBinder;

import aQute.bnd.exceptions.Exceptions;
import aQute.jpm.api.CommandData;
import aQute.jpm.api.JPM;
import aQute.jpm.api.JVM;
import aQute.jpm.lib.JustAnotherPackageManager;
import aQute.jpm.platform.PlatformImpl;
import aQute.lib.collections.ExtList;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.justif.Justif;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import aQute.libg.reporter.ReporterAdapter;
import aQute.struct.struct.Error;
import biz.aQute.result.Result;

/**
 * The command line interface to JPM
 */
@Description("Just Another Package Manager (for Java)\nMaintains a local repository of Java jars (apps or libs). Can automatically link these jars to an OS command or OS service. For more information see http://jpm.bndtools.org")
public class Main extends ReporterAdapter {
	private final static Logger	logger			= LoggerFactory.getLogger(Main.class);
	static Pattern				ASSIGNMENT		= Pattern.compile("\\s*([-\\w\\d_.]+)\\s*(?:=\\s*([^\\s]+)\\s*)?");
	public final static Pattern	URL_PATTERN		= Pattern.compile("[a-zA-Z][0-9A-Za-z]{1,8}:.+");
	public final static Pattern	BSNID_PATTERN	= Pattern.compile("([-A-Z0-9_.]+?)(-\\d+\\.\\d+.\\d+)?",
			Pattern.CASE_INSENSITIVE);
	File						base			= new File(System.getProperty("user.dir"));

	JPM							jpm;
	final PrintStream			err;
	final PrintStream			out;

	File						sm;
	JpmOptions					options;
	static String				encoding		= System.getProperty("file.encoding");
	int							width			= 120;																// characters
	int							tabs[]			= {
			40, 48, 56, 64, 72, 80, 88, 96, 104, 112
	};

	static {
		if (encoding == null)
			encoding = Charset.defaultCharset()
					.name();
	}

	/**
	 * Default constructor
	 *
	 * @throws UnsupportedEncodingException
	 */

	public Main() throws UnsupportedEncodingException {
		super(new PrintStream(System.err, true, encoding));
		err = new PrintStream(System.err, true, encoding);
		out = new PrintStream(System.out, true, encoding);
	}

	/**
	 * Main entry
	 *
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		Main jpm = new Main();
		try {
			jpm.run(args);
		} finally {
			jpm.err.flush();
			jpm.out.flush();
		}
	}

	public interface ModifyCommand {
		@Description("Provide or override the JVM arguments")
		String vmargs();

		@Description("Provide the vm JAVAHOME directory")
		String vm();

		@Description("Provide the path to the java executable")
		String java();

		@Description("Provide the name of the main class used to launch this command or service in fully qualified form, e.g. aQute.main.Main")
		String main();

		@Description("Provide the name of the command")
		String name();

		@Description("Provide the title of the command")
		String title();

		@Description("Java is default started in console mode, you can specify to start it in windows mode (or javaw)")
		boolean windows();

		@Description("Provide a range for the JVM. This must be in the OSGi syntax & semantics. I.e. "
				+ "simple version like 1.17 indicates higher than 1.17. However, the upper range can "
				+ "be limited with a range like `[1.8,1.11)`. If a range is specified, the command will fail "
				+ "if no matching vm can be found. If the `vm` is set, then this range option is ignored.")
		String range();
	}

	/**
	 * Commands
	 */
	@Arguments(arg = "[command]")
	@Description("Manage the commands that have been installed so far")
	public interface CommandOptions extends Options, ModifyCommand {

		@Description("Update the command")
		boolean update();
	}

	@Description("Remove jpm and all created data from the system (including commands and services). "
			+ "Without the --force flag only list the elements that would be deleted.")
	public interface deinitOptions extends Options {

		@Description("Actually remove jpm from the system")
		boolean force();
	}

	/**
	 * Main options
	 */

	@Arguments(arg = "cmd ...")
	@Description("Options valid for all commands. Must be given before sub command")
	interface JpmOptions extends Options {

		@Description("Print exception stack traces when they occur.")
		boolean exceptions();

		@Description("Trace on.")
		boolean trace();

		@Description("Be pedantic about all details.")
		boolean pedantic();

		@Description("Specify a new base directory (default working directory).")
		String base();

		@Description("Do not return error status for error that match this given regular expression.")
		String[] failok();

		@Description("Specify the home directory of jpm. (can also be permanently set with 'jpm settings jpm.home=...'")
		String home();

		@Description("Specify executables directory (one-shot)")
		String bindir();

		@Description("Wait for a key press, might be useful when you want to see the result before it is overwritten by a next command")
		boolean key();

		@Description("Show the release notes")
		boolean release();

		@Description("Specify the platform (this is mainly for testing purposes). Is either WINDOWS, MACOS, or LINUX")
		PlatformImpl.Type os();

		@Description("Specify an additional list of URLs to search. URLs must be comma separated and quoted")
		String urls();

		@Description("Show debug output")
		boolean debug();

		@Description("Width used for formatting")
		int width();
	}

	/**
	 * Initialize the repository and other global vars.
	 *
	 * @param opts
	 *            the options
	 * @throws IOException
	 */
	@Description("Just Another Package Manager for Java (\"jpm help jpm\" to see a list of global options)")
	public void _jpm(JpmOptions opts) throws IOException {

		try {
			setExceptions(opts.exceptions());
			setPedantic(opts.pedantic());
			if (opts.trace()) {
				setTrace(opts.trace());
			}
			if (opts.debug()) {
				StaticLoggerBinder.getSingleton()
						.add("*", Level.DEBUG);
			}

			if (opts.base() != null)
				base = IO.getFile(base, opts.base());

			File homeDir = IO.getFile("~/.jpm");
			File binDir;

			if (opts.home() != null) {
				homeDir = IO.getFile(base, opts.home());
			}

			if (opts.bindir() != null) {
				binDir = new File(opts.bindir());
				binDir = binDir.getAbsoluteFile();
			} else {
				binDir = new File(homeDir, "bin");
			}
			if (!binDir.isAbsolute())
				binDir = new File(base, opts.bindir());

			logger.debug("home={}, bin={}", homeDir, binDir);

			PlatformImpl platform = PlatformImpl.getPlatform(this, opts.os(), homeDir);
			logger.debug("Platform {}", platform);

			jpm = new JustAnotherPackageManager(this, platform, homeDir, binDir);

			checkPath();

			try {
				this.options = opts;

				CommandLine handler = opts._command();
				List<String> arguments = opts._arguments();

				if (arguments.isEmpty()) {
					Justif j = new Justif();
					Formatter f = j.formatter();
					handler.help(f, this);
					err.println(j.wrap());
				} else {
					String cmd = arguments.remove(0);
					String help = handler.execute(this, cmd, arguments);
					if (help != null) {
						err.println(help);
					}
				}

				if (options.width() > 0)
					this.width = options.width();
			} finally {
				jpm.close();
			}
		}

		catch (Exception t) {
			Throwable tt = Exceptions.unrollCause(t);
			exception(tt, "%s", tt);
		} finally {
			// Check if we need to wait for it to finish
			if (opts.key()) {
				System.out.println("Hit a key to continue ...");
				System.in.read();
			}
		}

		if (!check(opts.failok())) {
			System.exit(getErrors().size());
		}
	}

	private void checkPath() {
		String PATH = System.getenv()
				.get("PATH");

		if (PATH != null) {
			boolean inPath = Strings.split(File.pathSeparator, PATH)
					.stream()
					.map(IO::getFile)
					.filter(f -> {
						return f.equals(jpm.getBinDir());
					})
					.findAny()
					.isPresent();
			if (!inPath) {
				warning("bin directory is not in path: %s", jpm.getBinDir());
			}
		}
	}

	/**
	 * Install a jar options
	 */
	@Arguments(arg = {
			"command"
	})
	@Description("Install a jar into the repository. If the jar defines a number of headers it can also be installed as a command and/or a service. "
			+ "If not, additional information such as the name of the command and/or the main class must be specified with the appropriate flags.")
	public interface installOptions extends ModifyCommand, Options {
		// @Description("Ignore command and service information")
		// boolean ignore(); // pl: not used

		@Description("Force overwrite of existing command")
		boolean force();

	}

	/**
	 * A better way to install
	 */

	@Description("Install an artifact from a url, file, or Maven central")
	public void _install(installOptions opts) throws Exception {

		String coordinate = opts._arguments()
				.get(0);

		Result<CommandData> result = jpm.createCommandData(coordinate);
		if (result.isErr()) {
			error("failed to install %s: %s", coordinate, result.getMessage());
			return;
		}

		CommandData cmd = result.unwrap();
		cmd.properties = opts._properties();

		updateCommandData(cmd, opts);

		List<aQute.struct.struct.Error> errors = cmd.validate();
		if (!errors.isEmpty()) {
			error("Command not valid");
			for (Error error : errors) {
				error("[%s] %s %s %s %s", error.code, error.description, error.path, error.failure, error.value);
			}
		} else {
			String r = jpm.saveCommand(cmd, opts.force());

			if (r != null) {
				error("[%s] %s", coordinate, r);
			}
		}
	}

	private boolean updateCommandData(CommandData data, ModifyCommand opts) throws Exception {
		boolean update = false;
		
		if (opts.java() != null) {
			data.java = opts.java();
			update=true;
		} else if (data.range != null) {
			JVM selectVM = jpm.selectVM(data.range);
			if (selectVM == null) {
				warning("No vm installed on your system for required version %s, available are %s", data.range,
						jpm.getVMs());
				data.range = null;
				data.java = "java";
			}
			update=true;
		}

		if (opts.main() != null) {
			data.main = opts.main();
			update = true;
		}
		if (opts.vmargs() != null) {
			data.jvmArgs = opts.vmargs();
			update = true;
		}

		if (opts.windows() != data.windows) {
			data.windows = opts.windows();
			update = true;
		}

		if (opts.vm() != null) {
			JVM vm = jpm.selectVM(opts.vm());
			if (vm != null) {
				data.java = (data.windows ? vm.javaw() : vm.java()).getAbsolutePath();
				update = true;
			} else
				error("could not find a vm for %s", opts.vm());
		} else if (opts.range() != null) {
			data.range = opts.range();
			update = true;
		}
		if (opts.name() != null) {
			data.name = opts.name();
			update = true;
		}
		if (opts.title() != null) {
			data.title = opts.title();
			update = true;
		}

		return update;
	}

	@Description("Update the jpm commands")
	public void _update(CommandOptions opts) throws Exception {

		if (opts._arguments()
				.isEmpty()) {
			print(jpm.getCommands());
			return;
		}

		String cmd = opts._arguments()
				.get(0);

		Result<CommandData> result = jpm.getCommand(cmd);
		if (result.isErr()) {
			error(result.getMessage());
		} else {
			CommandData data = result.unwrap();
			if (opts.update()) {
				result = jpm.createCommandData(data.coordinate);
				if (result.isErr()) {
					error(result.getMessage());
					return;
				}
				data = result.unwrap();
				jpm.rmCommand(data.name);
			}
			if (updateCommandData(data, opts) | opts.update()) {
				String r = jpm.saveCommand(data, true);
				if (r != null)
					error("Failed to update command %s: %s", cmd, r);
			}
			print(data);
		}
	}

	@Description("List the jpm commands")
	public void _list(Options opts) throws Exception {
		print(jpm.getCommands());
	}

	private void print(CommandData command) throws Exception {
		Justif j = new Justif(width, tabs);
		Formatter f = j.formatter();
		f.format("%n[%s]%n", command.name);
		f.format("%s\n\n", Strings.display(command.description, command.title));
		f.format("Coordinate\t1%s%n", command.coordinate);
		f.format("JVMArgs\t1%s%n", command.jvmArgs);
		f.format("Main class\t1%s%n", command.main);
		f.format("Install time\t1%s%n", new Date(command.time));
		f.format("Path\t1%s%n", command.bin);
		f.format("JRE\t1%s%n", Strings.display(command.range, "<default>"));
		f.format("java(w)\t1%s%n", Strings.display(command.java, "<default>"));
		list(f, "Dependencies", command.dependencies);

		out.append(j.wrap());
	}

	private void list(Formatter f, String title, List<?> elements) {
		if (elements == null || elements.isEmpty())
			return;

		f.format("[%s]\t1", title);
		String del = "";
		for (Object element : elements) {
			f.format("%s%s", del, element);
			del = "\f";
		}
		f.format("%n");
	}

	private void print(List<CommandData> commands) {
		Justif j = new Justif(width, tabs);
		Formatter f = j.formatter();
		for (CommandData command : commands) {
			f.format("%s\t1%s%n", command.name, Strings.display(command.description, command.title));
		}
		out.append(j.wrap());
	}

	@Description("Remove jpm from the system by deleting all artifacts and metadata")
	public void _deinit(deinitOptions opts) throws Exception {
		if (opts.force())
			jpm.deinit();
		else
			error("Please specify -f");
	}

	/**
	 * Main entry for the command line
	 *
	 * @param args
	 * @throws Exception
	 */
	public void run(String[] args) throws Exception {
		StaticLoggerBinder.getSingleton().reporter = this;
		CommandLine cl = new CommandLine(this);
		ExtList<String> list = new ExtList<>(args);
		String help = cl.execute(this, "jpm", list);
		check();
		if (help != null)
			err.println(help);
	}

	/**
	 * Setup jpm to run on this system.
	 */
	@Description("Install jpm on the current system")
	interface InitOptions extends Options {

		@Description("Provide or override the JVM location (for Windows only)")
		String vm();

	}

	@Description("Install jpm on the current system")
	public void _init(InitOptions opts) throws Exception {

		jpm.init();

		try {
			String s = System.getProperty("jpm.jar", System.getProperty("java.class.path"));
			if (s == null) {
				error("Cannot initialize because not clear what the command jar is from java.class.path: %s", s);
				return;
			}
			String parts[] = s.split(File.pathSeparator);
			s = parts[0];
			try {
				File f = new File(s).getAbsoluteFile();
				if (f.exists()) {
					CommandLine cl = new CommandLine(this);

					String help = null;

					if (opts.vm() != null) {
						help = cl.execute(this, "install", Arrays.asList("-f", "-v", opts.vm(), f.getAbsolutePath()));
					} else {
						help = cl.execute(this, "install", Arrays.asList("-f", f.getAbsolutePath()));
					}

					if (help != null) {
						error(help);
						return;
					}

					out.println("Home dir      " + jpm.getHomeDir());
					out.println("Bin  dir      " + jpm.getBinDir());
				} else
					error("Cannot find the jpm jar from %s", f);
			} catch (InvocationTargetException e) {
				exception(e.getTargetException(), "Could not install jpm, %s", e.getTargetException());
				if (isExceptions())
					e.printStackTrace();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Show platform
	 */
	@Arguments(arg = {
			"[cmd]", "..."
	})
	@Description("Show the name of the platform, or more specific information")
	public interface PlatformOptions extends Options {
		@Description("Show detailed information")
		boolean verbose();
	}

	/**
	 * Show the platform info.
	 *
	 * @param opts
	 * @throws IOException
	 * @throws Exception
	 */
	@Description("Show platform information")
	public void _platform(PlatformOptions opts) throws IOException, Exception {
		CommandLine cli = opts._command();
		List<String> cmds = opts._arguments();
		if (cmds.isEmpty()) {
			if (opts.verbose()) {
				Justif j = new Justif(80, 30, 40, 50, 60);
				jpm.getPlatform()
						.report(j.formatter());
				out.append(j.wrap());
			} else
				out.println(jpm.getPlatform()
						.getName());
		} else {
			String execute = cli.execute(jpm.getPlatform(), cmds.remove(0), cmds);
			if (execute != null) {
				out.append(execute);
			}
		}
	}

	/**
	 * Show all the installed VMs
	 */
	@Description("Manage installed VMs ")
	interface VMOptions extends Options {
		@Description("Add a vm")
		String add();
	}

	@Description("Manage installed VMs ")
	public void _vms(VMOptions opts) throws Exception {
		if (opts.add() != null) {
			File f = IO.getFile(base, opts.add())
					.getCanonicalFile();

			if (!f.isDirectory()) {
				error("No such directory %s to add a JVM", f);
			} else {
				jpm.getVM(f);
			}
		}

		SortedSet<JVM> vms = jpm.getVMs();

		for (JVM jvm : vms) {
			out.printf("%-30s %12s (%12s)  %-20s %-10s %s\n", jvm.name, jvm.version, jvm.platformVersion, jvm.vendor, jvm.os_arch, jvm.javahome);
		}

	}

	@Arguments(arg = {})
	@Description("Show the current version. The qualifier represents the build date.")
	interface VersionOptions extends Options {

	}

	/**
	 * Show the current version
	 *
	 * @throws IOException
	 */
	@Description("Show the current version of jpm")
	public void _version(VersionOptions options) throws IOException {
		Enumeration<URL> urls = getClass().getClassLoader()
				.getResources("META-INF/MANIFEST.MF");
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			logger.debug("found manifest {}", url);
			Manifest m = new Manifest(url.openStream());
			String name = m.getMainAttributes()
					.getValue("Bundle-SymbolicName");
			if (name != null && name.trim()
					.equals("biz.aQute.jpm")) {
				out.println(m.getMainAttributes()
						.getValue("Bundle-Version"));
				return;
			}
		}
		error("No version found in jar");
	}

	/**
	 * Handle the global settings
	 */
	@Description("Manage user settings of jpm (in ~/.jpm). Without argument, print the current settings. "
			+ "Can alse be used to create change a settings with \"jpm settings <key>=<value>\"")
	interface settingOptions extends Options {
		boolean clear();
	}

	@Description("Manage user settings of jpm (in ~/.jpm)")
	public void _settings(settingOptions opts) throws Exception {
		logger.debug("settings {}", jpm.getSettings());
		List<String> rest = opts._arguments();
		Map<String, String> settings = jpm.getSettings();
		if (opts.clear()) {
			settings.clear();
			logger.debug("clear {}", settings.entrySet());
		}

		if (rest.isEmpty()) {
			list(null, settings);
		} else {
			boolean set = false;
			for (String s : rest) {
				Matcher m = ASSIGNMENT.matcher(s);
				logger.debug("try {}", s);
				if (m.matches()) {
					logger.debug("matches {} {} {}", s, m.group(1), m.group(2));
					String key = m.group(1);
					Glob instr = key == null ? Glob.ALL : new Glob(key);
					List<String> select = settings.keySet()
							.stream()
							.filter(k -> instr.matches(k))
							.collect(Collectors.toList());

					String value = m.group(2);
					if (value == null) {
						logger.debug("list wildcard {} {} {}", instr, select, settings.keySet());
						list(select, settings);
					} else {
						logger.debug("assignment 	");
						settings.put(key, value);
						set = true;
					}
				} else {
					err.printf("Cannot assign %s\n", s);

				}
			}
			if (set) {
				logger.debug("saving");
				jpm.save();
			}
		}
	}

	private void list(Collection<String> keys, Map<String, String> map) {
		for (Entry<String, String> e : map.entrySet()) {
			if (keys == null || keys.contains(e.getKey()))
				out.printf("%-40s = %s\n", e.getKey(), e.getValue());
		}
	}

	/**
	 * Alternative for command -r {@code <commandName>}
	 */
	@Arguments(arg = {
			"glob"
	})
	@Description("Remove the specified command(s) from the system by specifying a glob on the name")
	interface UninstallOptions extends Options {
		@Description("Just show what will be removed, do not actually remove it")
		boolean dry();
	}

	@Description("Remove a command from the system")
	public void _rm(UninstallOptions opts) throws Exception {

		Glob g = new Glob(opts._arguments()
				.get(0));
		for (CommandData command : jpm.getCommands()) {
			if (g.finds(command.name) < 0) {
				continue;
			}
			if (!opts.dry())
				jpm.rmCommand(command.name);
			out.printf("RM %-20s %s%n", command.name, command.coordinate);
		}
	}

	/**
	 * Constructor for testing purposes
	 */
	public Main(JustAnotherPackageManager jpm) throws UnsupportedEncodingException {
		this();
		this.jpm = jpm;
	}

	/**
	 * Show a list of candidates from a coordinate
	 */
	@Arguments(arg = "program")
	@Description("Print out the revisions for a program specification (group:artifact)")
	interface RevisionOptions extends Options {

	}

	@Description("List the revisions for a program GAV")
	public void _revisions(RevisionOptions options) throws Exception {
		List<String> arguments = options._arguments();
		String gav = arguments.remove(0);
		Result<List<String>> revisions = jpm.getRevisions(gav);
		if (revisions.isErr()) {
			error(revisions.getMessage());
		} else {
			List<String> list = revisions.unwrap();
			out.println(Strings.join("\n", list));
		}
	}

	@Arguments(arg = {})
	@Description("maintain the urls")
	interface URLOptions extends Options {
		boolean snapshot();

		String[] remove();

		String[] add();
	}

	@Description("maintain the urls")
	public void _urls(URLOptions options) {
		Map<String, String> settings = jpm.getSettings();
		boolean donesomething = false;
		String key = options.snapshot() ? JPM.SNAPSHOT_URLS : JPM.RELEASE_URLS;
		String urls = settings.get(key);
		ExtList<String> l = new ExtList<>(Strings.splitQuoted(urls));
		if (options.remove() != null) {
			for (String s : options.remove()) {
				Glob glob = new Glob(s);
				l.removeIf(ss -> glob.finds(ss) >= 0);
				donesomething |= true;
			}
		}
		if (options.add() != null) {
			for (String s : options.add()) {
				List<String> split = Strings.split(s);
				for (String ss : split)
					try {
						URL uri = new URL(ss);
						l.add(uri.toString());
						donesomething |= true;
					} catch (MalformedURLException e) {
						error("malformed url %s : %s", ss, e.getMessage());
					}
			}
		}
		if (donesomething) {
			settings.put(key, Strings.join(l));
			jpm.save();
		}
		out.println(Strings.join("\n", l));
	}

	@Description("Search the maven central database. Modifiers can be used to narrow the search.\n"
			+ "\t1c:<name>  – Search for class name\n" //
			+ "\t1fc:<name> – Search for fully qualified class name\n" //
			+ "\t11:<sha>   – Search for a sha\n" //
			+ "\t1g:<id>    – Search for the group id\n" //
			+ "\t1a:<id>    – Search for the artifact id\n" //
			+ "")
	interface SearchOptions extends Options {
		int from();

		int pages();

	}

	@Description("Search maven central")
	public void _search(SearchOptions options) {
		Result<List<String>> l = jpm.search(Strings.join(" ", options._arguments()), options.from(), options.pages());
		if (l.isErr()) {
			error(l.getMessage());
			return;
		}
		out.println(Strings.join("\n", l.unwrap()));
	}

}
