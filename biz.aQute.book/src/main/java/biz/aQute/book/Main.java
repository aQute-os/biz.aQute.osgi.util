package biz.aQute.book;

import java.io.File;
import java.util.Arrays;

import aQute.lib.env.Env;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

public class Main extends Env {

	@Description("")
	@Arguments(arg = {})
	interface RunArguments extends Options {
		@Description("Set the base directory. All file references are relative to the base")
		String base();

		@Description("The output file, default console")
		String output();

		@Description("Dump expansion of docs")
		boolean xpansion();
	}

	@Description("Create an html file from one or more markdown files")
	public void _run(RunArguments args) throws Exception {

		if (args.base() != null) {
			File d = getFile(args.base());
			if (!d.isDirectory()) {
				error("No such directory %s", d);
				return;
			}
			setBase(d);
		}

		File f = getFile("book.bnd");
		if (f.isFile()) {
			setProperties(f);
		}

		for (String arg : args._arguments()) {
			if (arg.contains("=")) {
				int n = arg.indexOf('=');
				String key = Strings.trim(arg.substring(0, n));
				String value = Strings.trim(arg.substring(n + 1));
				setProperty(key, value);
			} else {
				f = getFile(arg);
				if (f.isFile()) {
					setProperties(f);
				} else {
					error("No such file " + f);
				}
			}
		}

		if (isOk()) {
			Generator g = new Generator(this);
			if (args.xpansion())
				g.xpansion();
			String generated = g.generate();
			String out = args.output();
			if (out == null)
				System.out.println(generated);
			else {
				File o = getFile(out);
				o.getParentFile()
					.mkdirs();
				IO.store(generated, o);
			}
			getInfo(g);
		}
	}

	public static void main(String args[]) throws Exception {
		Main main = new Main();

		CommandLine cmd = new CommandLine(main);

		String execute = cmd.execute(main, "run", Arrays.asList(args));
		if (execute != null)
			System.out.println(execute);

		main.report(System.out);
	}
}
