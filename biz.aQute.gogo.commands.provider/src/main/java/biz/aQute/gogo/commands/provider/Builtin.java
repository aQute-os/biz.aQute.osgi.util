package biz.aQute.gogo.commands.provider;

import static java.nio.file.Files.newOutputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;

import aQute.lib.converter.Converter;
import aQute.lib.fileset.FileSet;
import aQute.lib.io.IO;
import aQute.libg.glob.Glob;

/**
 * gosh built-in commands.
 */
public class Builtin {
	private final static Set<String>	KEYWORDS	= new HashSet<>(
		Arrays.asList("abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package",
			"synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements", "protected",
			"throw", "byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient",
			"catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class",
			"finally", "long", "strictfp", "volatile", "const", "float", "native", "super", "while"));

	static final String[]				functions	= {
		"format", "getopt", "new", "set", "tac", "type"
	};

	private static final String[]		packages	= {
		"java.lang", "java.io", "java.net", "java.util"
	};

	public Object _new(CommandSession session, Object name, Object[] argv) throws Exception {
		Class<?> clazz = null;

		if (name instanceof Class<?>) {
			clazz = (Class<?>) name;
		} else {
			clazz = loadClass(session, name.toString());
		}

		constructor: for (Constructor<?> c : clazz.getConstructors()) {

			Type[] types = c.getGenericParameterTypes();
			if (types.length != argv.length) {
				continue;
			}

			Object[] parameters = new Object[types.length];

			for (int i = 0; i < argv.length; ++i) {
				try {
					parameters[i] = Converter.cnv(types[i], argv[i]);
				} catch (Exception e) {
					continue constructor;
				}
			}

			try {
				return c.newInstance(parameters);
			} catch (InvocationTargetException ite) {
				// ignore, look for next
			}
		}

		throw new IllegalArgumentException(
			"can't coerce " + Arrays.asList(argv) + " to any of " + Arrays.asList(clazz.getConstructors()));
	}

	@Descriptor("Show the session variables")
	public Map<String, Object> set(CommandSession session,
	// @formatter:off

		@Descriptor("show all variables, including private variables starting with '.', s")
		@Parameter(absentValue = "false", presentValue="true", names = {
			"-a", "--all"
		})
		boolean all,

		@Descriptor("Globs, if one of the globs match, the variable is shown. Providing no globs will list all except for private variables")
		Glob ...globs

		// @formatter:on
	) {

		Map<String, Object> variables = getVariables(session);
		if (all)
			return variables;

		return variables.entrySet()
			.stream()
			.filter(e -> e.getKey()
				.startsWith("."))
			.filter(e -> {
				if (globs.length == 0)
					return true;
				for (Glob g : globs) {
					if (g.matches(e.getKey()))
						return true;
				}
				return false;

			})
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Descriptor("Set a session variable")
	public void set(CommandSession session,
	// @formatter:off
		@Descriptor("The variable name")
		String name,
		@Descriptor("The value of the variable")
		Object value

		// @formatter:on
	) {
		session.put(name, value);
	}

	@Descriptor("Get or get a session variable")
	public Object set(CommandSession session,
	// @formatter:off
		@Descriptor("The variable name")
		String name
		// @formatter:on
	) {
		return session.get(name);
	}

	@Descriptor("Reverse of `cat` (show content of file). This stores the content of the output. This replaces redirection when used with pipes. E.g. `echo foo | tac file.txt`")
	public void tac(CommandSession session,
	//@formatter:off

		@Descriptor("If the output file exists, append the output instead of creating a new file")
		@Parameter(absentValue = "false", presentValue="true", names = {
			"-a", "--append"
		})
		boolean append,

		@Descriptor("Show progress")
		@Parameter(absentValue = "false", presentValue="true", names = {
			"-p", "--progress"
		})
		boolean progress,

		@Descriptor("The output file. If this is relative, it will be resolved against the current Gogo directory")
		File	file
		//@formatter:off
		) throws IOException {

		Path path = makeAbsolute(session, file);

		OutputStream out = newOutputStream(path, append ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);

		List<Function<Character,String>> ops = new ArrayList<>();
		if ( progress )
			ops.add( progress(session) );

		copy(System.in, out, ops);
	}

	@Descriptor("Copy a file to stdout, the unix cat command")
    public void cat(CommandSession session,
    	// @formatter:off

  	  	@Descriptor("Number the non-blank output lines, starting at 0.")
    	@Parameter(absentValue="false", presentValue="true", names = {"-n", "--number"})
    	boolean number,

  	  	@Descriptor("Print non visible characters as \\uXXXX")
    	@Parameter(absentValue="false", presentValue="true", names = {"-u", "--unprintable"})
    	boolean unprintable,

  	  	@Descriptor("Squeeze multiple adjacent empty lines, causing the output to be single spaced.")
    	@Parameter(absentValue="false", presentValue="true", names = {"-s", "--squeeze"})
    	boolean squeeze,

  	  	@Descriptor("Print out each character in hex")
    	@Parameter(absentValue="false", presentValue="true", names = {"-h", "--hex"})
    	boolean hex,

    	@Descriptor("The input files, relative to the Gogo sessions current directory. If no file is specified, copy from stdin")
    	File [] inputs

    	//@formatter:on

	) throws Exception {
		List<Function<Character, String>> ops = getOps(number, unprintable, squeeze, hex);
		for (File input : inputs) {
			Path path = makeAbsolute(session, input);
			InputStream stream = IO.stream(path);
			copy(stream, System.out, ops);
		}
	}

	@Descriptor("Echo the arguments to the command line")
	public Object[] echo(Object[] args) {
		return args;
	}

	@Descriptor("Grep the outut for matching glob expressions")
	public boolean grep(CommandSession session,
	//@formatter:off

		@Descriptor("Select non-matching lines")
		@Parameter(absentValue="false", presentValue="true",names= {"-v", "--negate", "--invert-match"})
		boolean negate,

		@Descriptor("Ignore case")
		@Parameter(absentValue="false", presentValue="true",names= {"-i", "--ignore-case"})
		boolean ignoreCase,

		@Descriptor("Suppress all normal output")
		@Parameter(absentValue="false", presentValue="true",names= {"-q", "--silent"})
		boolean quiet,

		@Descriptor("Provide line & file name for matches")
	    @Parameter(absentValue="false", presentValue="true",names= {"-n", "--number"})
		boolean number,

		@Descriptor("A glob expression")
		String	 match,

		@Descriptor("Files to search, this can use ant like globbing for files")
		String...files

		//@formatter:on
	) throws IOException {

		int flags = 0;
		if (ignoreCase) {
			flags |= Pattern.CASE_INSENSITIVE;
		}
		Glob glob = new Glob(match, flags);
		Predicate<String> matches;
		if (negate) {
			matches = s -> glob.finds(s) < 0;
		} else {
			matches = s -> glob.finds(s) >= 0;
		}

		List<Function<Character, String>> ops = new ArrayList<>();
		ops.add(grep(matches));
		boolean found = false;
		if (files.length == 0)
			copy(System.in, System.out, ops);
		else {
			FileSet fileset = getFileset(session, files);
			for (File f : fileset.getFiles()) {
				InputStream in = IO.stream(f);
				grep(in, f.getAbsolutePath(), matches, quiet, number);
			}
		}
		return found;
	}

	private FileSet getFileset(CommandSession session, String[] files) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean grep(InputStream in, String name, Predicate<String> matches, boolean quiet, boolean number) throws IOException {
		BufferedReader reader = IO.reader(in);
		int count = 0;
		String line;
		boolean found=false;
		while ((line = reader.readLine()) != null) {
			if (matches.test(line)) {
				found = true;
				if ( !quiet) {
					if ( number) {
						System.out.printl
					}

				}
			}
		}

		// TODO Auto-generated method stub

	}

	private List<Function<Character, String>> getOps(boolean number, boolean unprintable, boolean squeeze,
		boolean hex) {
		List<Function<Character, String>> ops = new ArrayList<>();
		if (hex) {
			class Hex {
				char	count	= 0;
				String	del		= "";
			}
			Hex h = new Hex();
			ops.add((c) -> {
				String result = "";
				if (h.count % 16 == 0) {
					result = String.format("%s%04X", h.del, h.count);
					h.del = "\n";
				}
				return result.concat(String.format(" %04X", (char) c));
			});
		}

		if (number) {
			class Number {
				boolean	first	= true;
				int		line	= 0;
			}
			Number n = new Number();
			ops.add((c) -> {
				if (n.first) {
					return "   0 ";
				}
				if (c == '\n') {
					return String.format("\n%4d ", n.line++);
				}
				return c.toString();
			});
		}

		if (unprintable) {
			ops.add((c) -> {
				if (isPrintable(c)) {
					return c.toString();
				}
				return String.format("\\u04X", (char) c);
			});
		}

		if (squeeze) {
			class Squeeze {
				char last = 0;
			}
			Squeeze s = new Squeeze();
			ops.add((c) -> {
				if (c != '\n' || c != '\r' || s.last != '\n')
					return c.toString();

				if (c == '\r')
					return "\r";

				if (s.last == '\n') {
					return "";
				}
				s.last = c;
				return c.toString();
			});
		}
		return ops;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getVariables(CommandSession session) {
		try {
			Class<? extends CommandSession> clazz = session.getClass();
			Method method = clazz.getMethod("getVariables");
			return (Map<String, Object>) method.invoke(session);
		} catch (Exception e) {
			return Collections.emptyMap();
		}
	}

	private void copy(InputStream input, OutputStream out, List<Function<Character, String>> ops)
		throws IOException {
		int ch;
		while ((ch = input.read()) >= 0) {
			Character c = Character.valueOf((char) ch);
			String expand = expand(c, ops, 0);
			byte[] data = expand.getBytes(StandardCharsets.UTF_8);
			out.write(data);
		}
	}

	private String expand(Character c, List<Function<Character, String>> ops, int index) {

		if (index >= ops.size())
			return c.toString();

		Function<Character, String> op = ops.get(index);
		String s = op.apply(c);
		StringBuilder sb = new StringBuilder();
		for (int cindex = 0; cindex < s.length(); cindex++) {
			String expanded = expand(s.charAt(cindex), ops, index + 1);
			sb.append(expanded);
		}
		return sb.toString();
	}

	public boolean isPrintable(char c) {
		Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
		return (!Character.isISOControl(c)) && block != null && block != Character.UnicodeBlock.SPECIALS;
	}

	private Class<?> loadClass(CommandSession session, String name) throws ClassNotFoundException {
		if (!name.contains(".")) {
			for (String p : packages) {
				String pkg = p + "." + name;
				try {
					return Class.forName(pkg, true, session.classLoader());
				} catch (ClassNotFoundException e) {}
			}
		}
		return Class.forName(name, true, session.classLoader());
	}

	private Function<Character, String> progress(CommandSession session) {
		class Progress {
			int		count	= 0;
			long	last	= 0;
		}
		Progress p = new Progress();

		return (Character cc) -> {
			if (p.count % 1000 == 0) {
				long now = System.currentTimeMillis();
				if (now - p.last > 2000) {
					session.getConsole()
						.printf("%8d bytes\r", p.count);
					p.last = now;
				}
			}
			p.count++;
			return cc.toString();
		};
	}

	private Path makeAbsolute(CommandSession session, File file) {
		Path path = file.toPath();
		if (!file.isAbsolute()) {
			Path currentDir = session.currentDir();
			path = currentDir.resolve(path);
		}
		return path;
	}

}
