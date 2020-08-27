package biz.aQute.to;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.lib.env.Env;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.justif.Justif;
import aQute.lib.strings.Strings;
import aQute.libg.command.Command;

public class Main {

	static class To extends Env {

		@Description("Open a browser indirectly via property expansion in a .to file. You can place "
				+ "a .to file in the home directory of the current user and/or in the current working "
				+ "directory, or one of its parent directories. Properties in the working directory overrides the propertie with the same "
				+ "name in the home directory.\n"
				+ "A .to file is a properties file. The key is the the argument given to the "
				+ "to command. The value must be a URL. The ${1}..${9} macros are available for"
				+ "any further arguments. The ${0} macro is the name, the ${args} is a list of the"
				+ "arguments. Most of the bnd macros are supported in the expansion. For example, "
				+ "you can ${if;...;....;...} for conditions. \n"
				+ "Special macros:\n"
				+ "  ${dirname} short name of local directory\n"
				+ "  ${gitpath} path from the first .git ancestor directory. Always uses / as separator.")
		@Arguments(arg = { "[name]", "args..." })
		interface RunArguments extends Options {
			@Description("Show the URL that would be opened")
			boolean dry();

			@Description("List all property keys")
			boolean list();
		}

		@Description("Open a browser indirectly via property expansion in a .to file")
		public void _run(RunArguments args) throws Exception {
			Env local = new Env();
			File localFile = IO.getFile("~/.to");
			if (localFile.isFile()) {
				local.setProperties(localFile);
			}
			local.setProperty("_dir", "~");

			local = getEnv(local);

			local.setProperty("_dir", ".");
			local.setProperty("dirname", IO.work.getName());
			local.setProperty("gitpath", gitpath());
			local.setProperty("branch", command("git symbolic-ref --short HEAD"));

			try {

				local.setProperty("args", Strings.join(args._arguments()));
				List<String> arguments = args._arguments();

				if (args.list() || arguments.isEmpty()) {
					doList(local);
					return;
				}

				for (int i = 0; i < Math.max(10, arguments.size()); i++) {
					if (i < arguments.size()) {
						local.setProperty("" + i, arguments.get(i));
					} else {
						local.setProperty("" + i, "");
					}
				}

				if (arguments.isEmpty()) {
					error("to requires at least one argument");
				}
				URI uri;
				String name = local.getProperty(arguments.remove(0));
				if (name == null) {
					uri = new URI(local.getProperty(".search", "https://google.com/search?q=")
							+ Strings.join("+", arguments));
				} else {

					name = Strings.trim(name);
					try {
						System.out.println("'" + name + "'");
						uri = URI.create(name);
						if (!uri.isAbsolute()) {
							return;
						}
					} catch (Exception e) {
						error("Cannot turn into a url %s", e.getMessage());
						return;
					}
				}
				if (args.dry()) {
					System.out.println(uri);
				} else {
					java.awt.Desktop.getDesktop().browse(uri);
				}
			} finally {
				getInfo(local);
			}
		}

		private String command(String string) throws Exception {
			Command command = new Command(string);
			StringBuilder out = new StringBuilder();
			int execute = command.execute(out, out);
			if ( execute != 0) {
				return "<could not execute command " + string + " : "+execute+"> ";
			}
				
			return Strings.trim(out.toString());
		}

		private void doList(Env local) {
			getAll(local).entrySet().stream().sorted((a, b) -> a.getKey().compareTo(b.getKey())).forEach((e) -> {
				System.out.printf("%-40s %s%n", e.getKey(), e.getValue());
			});
			return;
		}

		private Map<String, String> getAll(Env local) {
			Map<String, String> all;
			if (local.getParent() == null) {
				all = new HashMap<>();
			} else {
				all = local.getParent().getMap();
			}
			all.putAll(local.getMap());
			return all;
		}

		private String gitpath() {
			File dir = IO.work;
			String path = "";
			while (dir != null && dir.isDirectory()) {
				File dotgit = new File(dir, ".git");
				if (dotgit.isDirectory()) {
					return path;
				}
				path = (path.isEmpty() ? dir.getName() : dir.getName() + "/" + path);
				dir = dir.getParentFile();
			}
			return path;
		}

		private Env getEnv(Env parent) throws Exception {
			File rover = IO.work;
			int n = 0;
			while (rover != null && rover.isDirectory()) {
				File localFile = new File(rover, ".to");
				if (localFile.isFile()) {
					parent = new Env(parent);
					parent.setProperties(localFile);
					parent.setProperty("_dir", calcDir(n));
				}
				rover = rover.getParentFile();
				n++;
			}
			return parent;
		}

		private String calcDir(int n) {
			if (n == 0)
				return ".";
			else if (n == 1) {
				return "..";
			} else {
				return calcDir(n - 1) + "/..";
			}
		}

	}

	public static void main(String[] args) throws Exception {

		To main = new To();
		CommandLine cmd = new CommandLine(main);

		String execute = cmd.execute(main, "run", Arrays.asList(args));
		if (execute != null)
			System.out.println(execute);

		main.report(System.out);
	}

}
