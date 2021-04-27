package biz.aQute.gogo.commands.provider;

import static java.nio.file.Files.newOutputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import aQute.lib.converter.Converter;
import aQute.lib.fileset.FileSet;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import biz.aQute.gogo.commands.dtoformatter.DTOFormatter;

/**
 * gosh built-in commands.
 */
public class Builtin {

	private static final String		EXCEPTION	= "exception";

	private static final String[]	packages	= {
		"java.lang", "java.io", "java.net", "java.util"
	};

	final List<String>				imports		= new ArrayList<>();

	{
		imports.addAll(Arrays.asList(packages));
	}

	final BundleContext	context;
	final DTOFormatter	dtof;

	public Builtin(BundleContext context, DTOFormatter dtof) {
		this.context = context;
		this.dtof = dtof;
		dtof.build(Class.class)
			.inspect()
			.method("name")
			.format("bundle", this::loader)
			.format("super", Class::getSuperclass)
			.line()
			.method("name")
			.format("bundle", this::loader)
			.part()
			.as(c -> c.getName());

	}

	/**
	 * NEW
	 */

	@Descriptor("Create a new object, constructor parameters can be passed")
	public Object _new(CommandSession session,
	//@formatter:off

		@Parameter(presentValue = "true", absentValue = "false", names= {"-a", "--accessible"})
		@Descriptor("Make the constructor accessible")
		boolean accessible,

		@Descriptor("The class or its name")
		Object _class,

		@Descriptor("The constructor arguments. The first matching constructor is used")
		Object ...argv
	//@formatter:on

	) throws Throwable {
		Class<?> clazz = null;

		if (_class instanceof Class<?>) {
			clazz = (Class<?>) _class;
		} else {
			clazz = loadClass(session, _class.toString());
		}

		constructor: for (Constructor<?> c : clazz.getConstructors()) {

			if (!accessible && !c.isAccessible()) {
				continue constructor;
			}

			c.setAccessible(accessible);

			if (c.getParameterCount() != argv.length)
				continue;

			java.lang.reflect.Parameter[] parameters = c.getParameters();

			for (int i = 0; i < c.getParameterCount(); i++) {
				Object value = argv[i];
				Class<?> type = parameters[i].getType();
				if (!type.isInstance(value))
					continue constructor;
			}

			try {
				return c.newInstance(argv);
			} catch (InvocationTargetException ite) {
				throw ite.getTargetException();
			}
		}

		// Try to match by converting parameters

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

	/**
	 * IMPORT
	 */

	@Descriptor("Add to the imports that are searched for loadclass")
	public List<String> imports(String... packages) {
		if (packages.length == 0)
			return imports;

		imports.addAll(Arrays.asList(packages));
		return Collections.emptyList();
	}

	/**
	 * VARS
	 */
	@Descriptor("Show the session variables")
	public Object vars(CommandSession session,
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
		if (all && globs.length == 0)
			return variables;

		variables = variables.entrySet()
			.stream()
			.filter(e -> !e.getKey()
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
		if (globs.length == 1) {
			return variables.values()
				.iterator()
				.next();
		}
		return variables;
	}

	/**
	 * TAC
	 */

	@Descriptor("Copy the input to a file")
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
    	File ... inputs

    	//@formatter:on

	) throws Exception {
		List<Function<Character, String>> ops = getOps(number, unprintable, squeeze, hex);
		if (inputs.length == 0)
			copy(System.in, System.out, ops);
		for (File input : inputs) {
			Path path = makeAbsolute(session, input);
			InputStream in = IO.stream(path);
			copy(in, System.out, ops);
		}
		System.out.println();
	}

	@Descriptor("Echo the arguments to the command line")
	public void echo(
	// @formatter:off

		@Parameter(absentValue="false", presentValue="true",names={"-s","--skip"})
	    @Descriptor("Skip space between arguments")
		boolean skip,

		@Descriptor("Any arguments to print. Arguments will be printed with a space separator unless -s is set")
		Object... args

	// @formatter:on

	) {
		String del = "";
		for (Object o : args) {
			o = unescape(o);
			System.out.printf("%s%s", del, o);
			if (!skip)
				del = " ";
		}
		System.out.println();
	}

	@Descriptor("Return a unicode character based on a hex value or named")
	public char u(

		@Descriptor("The hexaddecimal value for a character or one of tab,cr,bs,ff,lf,nl,esc")
		String hex) {
		switch (hex.toLowerCase()) {
			case "tab" :
				return '\t';
			case "cr" :
				return '\r';
			case "bs" :
				return '\b';
			case "ff" :
				return '\f';
			case "lf" :
			case "nl" :
				return '\n';
			case "esc" :
				return '\u001B';
		}
		return (char) Integer.parseInt(hex, 16);
	}

	@Descriptor("Grep input or files for matching glob expressions")
	public boolean grep(CommandSession session,
	//@formatter:off

		@Descriptor("Select non-matching lines")
		@Parameter(absentValue="false", presentValue="true",names= {"-v", "--negate", "--invert-match"})
		boolean negate,

		@Descriptor("Show relative filename")
		@Parameter(absentValue="false", presentValue="true",names= {"-r", "--relative"})
		boolean relative,

		@Descriptor("Ignore case")
		@Parameter(absentValue="false", presentValue="true",names= {"-i", "--ignorecase"})
		boolean ignorecase,

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
		if (ignorecase) {
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
		boolean found = false;
		if (files.length == 0)
			found = grep(System.in, System.out, matches, quiet, number ? "" : null);
		else {
			Collection<File> fileset = files(session, session.currentDir()
				.toFile(), files);
			for (File f : fileset) {
				InputStream in = IO.stream(f);
				found |= grep(in, System.out, matches, quiet,
					number ? (relative ? f.getName() : f.getAbsolutePath()) : null);
			}
		}
		return found;
	}

	@Descriptor("Map each element")
	public List<Object> each(CommandSession session,
	//@formatter:off
		@Descriptor("A collection or array of any object passed as the the first argument ($1 or $it). This function is normally referred to as 'map'")
		Collection<Object> list,
		@Descriptor("The closure that is executed for each object in the list. The closure can use $it to refer to the object. The resulting value will be returned in a list at the same position as $it")
		org.apache.felix.service.command.Function closure

		//@formatter:on

	) throws Exception {

		List<Object> results = new ArrayList<>();

		for (Object x : list) {
			Object value = closure.execute(session, Collections.singletonList(x));
			results.add(value);
		}

		return results;
	}

	@Descriptor("Filter a list")
	public List<Object> filter(CommandSession session,
	//@formatter:off
		@Descriptor("A collection or array of any object passed as the the first argument ($1 or $it). This function is normally referred to as 'map'")
		Collection<Object> list,
		@Descriptor("The closure that is executed for each object in the list. The closure can use $it to refer to the object. The resulting value will be returned in a list at the same position as $it")
		org.apache.felix.service.command.Function closure

		//@formatter:on

	) throws Exception {

		List<Object> results = new ArrayList<>();

		for (Object x : list) {
			Object value = closure.execute(session, Collections.singletonList(x));
			if (isTrue(value))
				results.add(x);
		}

		return results;
	}

	@Descriptor("if condition if-action else-action –– The condition and actions can be a normal object or a function. In the case of a function it is executed and the resulting value is used.")
	public Object _if(CommandSession session,
	//@formatter:off

		@Descriptor("The condition. If a function, it will be executed and the result is used")
		Object condition,

		@Descriptor("The result when the condition is true. If a function, it will be executed and the result is used")
		Object action,

		@Descriptor("The result when the condition is false. If a function, it will be executed and the result is used")
		Object elseAction

	//@formatter:on
	) throws Exception {

		if (isTrue(execute(session, condition)))
			return execute(session, action);
		else
			return execute(session, elseAction);
	}

	@Descriptor("if condition if-action –– The condition and actions can be a normal object or a function. In the case of a function it is executed and the resulting value is used. If the condition is false, null is returned")
	public Object _if(CommandSession session,

	//@formatter:off

		@Descriptor("The condition. If a function, it will be executed and the result is used")
		Object condition,

		@Descriptor("The result when the condition is true. If a function, it will be executed and the result is used")
		Object action

		//@formatter:on

	) throws Exception {
		return _if(session, condition, action, null);
	}

	@Descriptor("a greater than b")
	public boolean gt(String a, String b) {
		return a.compareTo(b) > 0;
	}

	@Descriptor("a greater than b")
	public boolean gt(long a, long b) {
		return a > b;
	}

	@Descriptor("Greater Than or Equal")
	public boolean gte(long n1, long n2) {
		return n1 >= n2;
	}

	@Descriptor("Greater Than or Equal")
	public boolean gte(String n1, String n2) {
		return n1.compareTo(n2) >= 0;
	}

	@Descriptor("Greater Than or Equal")
	public boolean gte(double n1, double n2, double epsilon) {
		return n1 > (n2 - epsilon) || eq(n1, n2, epsilon);
	}

	@Descriptor("Equal with epsilon")
	public boolean eq(double n1, double n2, double epsilon) {
		return n1 > n2 - epsilon && n1 < n2 + epsilon;
	}

	@Descriptor("Compare equal")
	public boolean eq(long n1, long n2) {
		return n1 == n2;
	}

	@Descriptor("Compare equal")
	public boolean eq(@Descriptor("Ignore case")
	@Parameter(absentValue = "false", presentValue = "true", names = {
		"-i", "--ignorecase"
	})
	boolean ignorecase, String n1, String n2) {
		if (ignorecase)
			return n1.toLowerCase()
				.compareTo(n2.toLowerCase()) == 0;
		else
			return n1.compareTo(n2) == 0;

	}

	@Descriptor("Less Than")
	public boolean lt(long n1, long n2) {
		return n1 < n2;
	}

	@Descriptor("Less than")
	public boolean lt(String n1, String n2) {
		return n1.compareTo(n2) < 0;
	}

	@Descriptor("Less Than or Equal")
	public boolean lte(long n1, long n2) {
		return n1 <= n2;
	}

	@Descriptor("Less than or Equal")
	public boolean lte(String n1, String n2) {
		return n1.compareTo(n2) <= 0;
	}

	@Descriptor("Add numbers together")
	public long plus(

		@Descriptor("Numbers to add")
		long... others) {
		long acc = 0;
		for (int i = 0; i < others.length; i++)
			acc += others[i];
		return acc;
	}

	@Descriptor("Subtract")
	public long sub(long n1, long n2) {
		return n1 - n2;
	}

	@Descriptor("Multiply")
	public long mul(long... others) {
		long acc = 1;
		for (int i = 0; i < others.length; i++)
			acc *= others[i];
		return acc;
	}

	@Descriptor("Divide")
	public long div(long n1, long n2) {
		return n1 / n2;
	}

	@Descriptor("Modulo")
	public Object mod(long n1, long n2) {
		return n1 % n2;
	}

	@Descriptor("Not a Number (NaN)")
	public double nan() {
		return Double.NaN;
	}

	@Descriptor("not expr –– The expression can be a normal object or a function. In the case of a function it is executed and the resulting value is used.")
	public boolean not(CommandSession session, Object value) throws Exception {
		return !isTrue(execute(session, value));
	}

	@Descriptor("throw a message as an IllegalArgumentException")
	public void _throw(

	//@formatter:off

		@Descriptor("The message to throw.")
		String message

		//@formatter:on
	) {
		throw new IllegalArgumentException(message);
	}

	@Descriptor("throw the last throw exception")
	public void _throw(CommandSession session) throws Throwable {
		Object exception = session.get(EXCEPTION);
		if (exception instanceof Throwable)
			throw (Throwable) exception;
		else
			throw new IllegalArgumentException("exception not set or not Throwable.");
	}

	@Descriptor("run a function but catch any exceptions. If an exception is thrown, it is placed in `exception`. If an exception is thrown, null will be returned")
	public Object _try(CommandSession session,

	//@formatter:off

		@Descriptor("The function will be called  but any exceptions are ignored.")
		org.apache.felix.service.command.Function func

		//@formatter:off
		) {


		try {
			return execute(session, func);
		} catch (Exception e) {
			session.put(EXCEPTION, e);
			return null;
		}
	}

	@Descriptor("run a function and catch the exceptions. If an exception is thrown, it is placed in `exception` and passed as a parameter to the error function")
	public Object _try(CommandSession session,
		//@formatter:off

		@Descriptor("The function will be called  but any exceptions are forwarded to the error, if it is a function")
		org.apache.felix.service.command.Function func,

		@Descriptor("Return value when an exception is thrown. If error is a function, it is called and passed the exception thrown")
		Object error

		//@formatter:off
		) throws Exception {
		try {
			return execute(session, func);
		} catch (Exception e) {
			session.put(EXCEPTION, e);
			return execute(session, error, e);
		}
	}

	@Descriptor("Run a function while the executed condition returns true")
	public void _while(CommandSession session,
		//@formatter:off

		@Descriptor("The condition function, repeats as long as this returns true")
		org.apache.felix.service.command.Function condition,

		@Descriptor("The body function, repeats as long as the condtions is true")
		org.apache.felix.service.command.Function ifTrue

		//@formatter:off
		) throws Exception {
		while (isTrue(condition.execute(session, null))) {
			ifTrue.execute(session, null);
		}
	}


	@Descriptor("Excecute a script from a URL")
	public void source(CommandSession session,
		//@formatter:off

		@Parameter(absentValue="false", presentValue="true", names= {"-x", "--xtrace"})
		@Descriptor("Trace the commands while executing")
		boolean xtrace,

		@Descriptor("The url to source the script from")
		URL url
		//@formatter:off
		) throws Exception {
		source0(session, xtrace, IO.collect(url.openStream()));
	}

	@Descriptor("Excecute scripts from files. File names are relative from the current Gogo dir. If no files are specified, the script is read from System.in")
	public void source(CommandSession session,
		//@formatter:off

		@Parameter(absentValue="false", presentValue="true", names= {"-x", "--xtrace"})
		@Descriptor("Trace the commands while executing")
		boolean xtrace,

		@Descriptor("File specifications. It is possible to use ant like file set expressions. If no files are specified, System.in is read")
		String... files

		//@formatter:on
	) throws Exception {
		if (files.length == 0) {
			source0(session, xtrace, IO.collect(System.in));
		} else {
			Collection<File> fs = files(session, session.currentDir()
				.toFile(), files);

			for (File f : fs) {
				source0(session, xtrace, IO.collect(f));
			}
		}
	}

	@Descriptor("Excecute scripts from files. File names are relative from the current Gogo dir. If no files are specified, the script is read from System.in")
	public void source(CommandSession session,
	//@formatter:off
			@Parameter(absentValue="false", presentValue="true", names= {"-x", "--xtrace"})
			@Descriptor("Trace the commands while executing")
			boolean xtrace,

			@Descriptor("A line to execute")
			String source
		//@formatter:on
	) throws Exception {
		source0(session, xtrace, source);
	}

	@Descriptor("Return an array of longs")
	public long[] range(long lowInclusive, long highExclusive) {
		if (lowInclusive >= highExclusive)
			return new long[0];

		long[] result = new long[(int) (highExclusive - lowInclusive)];
		for (int i = 0; i < result.length; i++)
			result[i] = i + lowInclusive;

		return result;

	}

	@Descriptor("Add a service to the variables. You can then use this service to invoke its methods as commands. This service is not tracked so there  is a checked out copy thatt can disappear")
	public Object service(String service) throws Exception {
		ServiceReference<?>[] refs = context.getServiceReferences((String) null, "(objectClass=*" + service + ")");
		if (refs == null) {
			System.out.println("No such service");
			return null;
		}

		if (refs.length > 1) {
			System.out.println("Multiple services");
			return null;
		}

		ServiceReference<?> ref = refs[0];

		return context.getService(ref);
	}

	@Descriptor("repeat a command until a key is hit")
	public void repeat(
	//@formatter:off

		CommandSession	session,

		@Descriptor("Repeat period in seconds, must be > 0. Default is 1")
		@Parameter(absentValue = "1", names = {"-p","--period"} )
		int period,

		@Descriptor("Update screen with the result.")
		@Parameter(absentValue = "false", presentValue="true", names = {"-u","--update"} )
		boolean update,

		String ... args
		//@formatter:on
	) throws Exception {
		String line = Stream.of(args)
			.collect(Collectors.joining(" "));

		while (true) {
			if (update)
				cls(session);

			Object execute = session.execute(line);

			String format = session.format(execute, org.apache.felix.service.command.Converter.INSPECT)
				.toString();
			session.getConsole()
				.println(format);
			session.getConsole()
				.flush();

			if (anykey(session, period) != 0)
				return;
		}

	}

	@Descriptor("Show the amount of free memory")
	public long freeMemory(
		//@formatter:off
		@Descriptor("Run a gc first")
		@Parameter(absentValue = "false", presentValue="true", names = {"-g","--gc"} )
		boolean gc
		//@formatter:on
		) {
		if ( gc )
			System.gc();

		return Runtime.getRuntime()
			.freeMemory();
	}

	@Descriptor("Return a key that is hit")
	public int anykey(
	//@formatter:off

		CommandSession	session,

		@Descriptor("Repeat period in seconds, must be > 0. Default is 1")
		@Parameter(absentValue = "1", names = {"-p","--period"} )
		int period

		//@formatter:on

	) throws IOException, InterruptedException {

		long deadline = System.currentTimeMillis() + period * 1000;
		while (System.currentTimeMillis() < deadline) {
			if (session.getKeyboard()
				.available() > 0) {
				return session.getKeyboard()
					.read();
			}
			Thread.sleep(100);
		}
		return 0;
	}

	@Descriptor("Clear the screen")
	public void cls(CommandSession session) {
		session.getConsole()
			.append("\u001B[2J")
			.flush();
	}

	@Descriptor("Provide a null value")
	public Object _null() {
		return null;
	}

	private void source0(CommandSession session, boolean xtrace, String script) throws Exception {
		String replace = script.replace("\\\r?\n", " ");
		for (String line : Strings.splitLines(replace)) {
			if (xtrace)
				System.out.println("x: " + line);
			session.execute(line);
		}
	}

	private Object execute(CommandSession session, Object value, Object... args) throws Exception {
		if (value instanceof org.apache.felix.service.command.Function) {
			return ((org.apache.felix.service.command.Function) value).execute(session, Arrays.asList(args));
		}
		return value;
	}

	@Descriptor("Get files relative to the current directory based on ant like expressions")
	public List<File> files(CommandSession session, String files) {
		return files(session, session.currentDir()
			.toFile(), files);

	}

	@Descriptor("Get files relative to the given directory based on ant like expressions")
	public List<File> files(CommandSession session, File base, String files) {
		return Stream.of(files)
			.flatMap(f -> new FileSet(base, f).getFiles()
				.stream())
			.collect(Collectors.toList());
	}

	private Collection<File> files(CommandSession session, File base, String[] files) {
		List<File> result = new ArrayList<>();
		for (String spec : files) {
			result.addAll(files(session, base, spec));
		}
		return result;
	}

	private boolean grep(InputStream in, PrintStream out, Predicate<String> matches, boolean quiet, String fileName)
		throws IOException {

		BufferedReader reader = IO.reader(in);
		int count = 0;
		String line;
		boolean found = false;
		while ((line = reader.readLine()) != null) {
			if (matches.test(line)) {
				found = true;
				if (!quiet) {
					if (fileName != null) {
						out.printf("%s:%04d | ", fileName, count);
					}
					out.println(line);
				}
			}
			count++;
		}
		return found;
	}

	private List<Function<Character, String>> getOps(boolean number, boolean unprintable, boolean squeeze,
		boolean hex) {
		List<Function<Character, String>> ops = new ArrayList<>();
		if (hex) {
			class Hex {
				int		count	= 0;
				String	del		= "";
			}
			Hex h = new Hex();
			ops.add((c) -> {
				String result = "";
				if (h.count % 16 == 0) {
					result = String.format("%s%04X", h.del, h.count);
					h.del = "\n";
				}
				h.count++;
				return result.concat(String.format(" %04X", (int) c));
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
					n.first = false;
					n.line++;
					return "   0 " + c;
				} else {
					if (c == '\n') {
						return String.format("\n%4d ", n.line++);
					}
				}
				return c.toString();
			});
		}

		if (unprintable) {
			ops.add((c) -> {
				if (isPrintable(c)) {
					return c.toString();
				}
				return String.format("\\u%04X", (char) c);
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

	private void copy(InputStream input, OutputStream out, List<Function<Character, String>> ops) throws IOException {
		int ch;
		while ((ch = input.read()) >= 0) {
			Character c = Character.valueOf((char) ch);
			String expand = expand(c, ops, 0);
			byte[] data = expand.getBytes(StandardCharsets.UTF_8);
			out.write(data);
		}
		out.flush();
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

	@Descriptor("Load a class by its name. Standard java packages are already imported.")
	public Class<?> loadClass(CommandSession session, String name) throws ClassNotFoundException {
		if (!name.contains(".")) {
			for (String p : imports) {
				String pkg = p + "." + name;
				try {
					return Class.forName(pkg, true, session.classLoader());
				} catch (ClassNotFoundException e) {}
			}
		}
		return Class.forName(name, true, Builtin.class.getClassLoader());
	}

	@Descriptor("Load a class by its name from a bundle")
	public Class<?> loadClass(CommandSession session,
	//@formatter:off


		@Descriptor("The bundle to load from")
		Bundle bundle,

		@Descriptor("The class name")
		String name
		//@formatter:on

	) throws ClassNotFoundException {
		try {
			return bundle.loadClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
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

	private boolean isTrue(Object result) throws InterruptedException {
		checkInterrupt();

		if (result == null)
			return false;

		if (result instanceof Boolean)
			return (Boolean) result;

		if (result instanceof Number) {
			if (0 == ((Number) result).intValue())
				return false;
		}

		if ("".equals(result))
			return false;

		return !"0".equals(result);
	}

	private void checkInterrupt() throws InterruptedException {
		if (Thread.currentThread()
			.isInterrupted())
			throw new InterruptedException("loop interrupted");
	}

	private Bundle loader(Class<?> class1) {
		return FrameworkUtil.getBundle(class1);
	}

	private Object unescape(Object o) {
		if (!(o instanceof String))
			return o;

		StringBuilder sb = new StringBuilder(o.toString());
		for (int i = 0; i < sb.length(); i++) {
			char ch = sb.charAt(i);
			switch (ch) {
				case '\\' :
					sb.delete(i, i + 1);
					ch = sb.charAt(i);
					if (i < sb.length()) {
						switch (ch) {
							case 't' :
								sb.setCharAt(i, '\t');
								break;

							case 'b' :
								sb.setCharAt(i, '\b');
								break;

							case 'n' :
								sb.setCharAt(i, '\n');
								break;

							case 'r' :
								sb.setCharAt(i, '\r');
								break;

							case 'f' :
								sb.setCharAt(i, '\f');
								break;

							case '"' :
								sb.setCharAt(i, '"');
								break;

							case '\'' :
								sb.setCharAt(i, '\'');
								break;
							case '\\' :
								sb.setCharAt(i, '\\');
								break;

						}
					}
			}

		}
		return sb.toString();

	}

}
