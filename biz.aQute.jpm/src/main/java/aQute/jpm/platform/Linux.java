package aQute.jpm.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.jpm.api.JVM;
import aQute.lib.io.IO;

class Linux extends Unix {
	// /etc/alternatives/java
	final static String[]		VMDIRS					= { "/usr/lib/jvm", "/usr/java/", "/usr/local/java/jdk",
			"/usr/lib64/jvm" };

	private static final String	PATH_SEPARATOR			= Pattern.quote(File.pathSeparator);
	static final String			COMPLETION_DIRECTORY	= "/etc/bash_completion.d";
	private final static Logger	logger					= LoggerFactory.getLogger(Linux.class);

	Linux(File cache) {
		super(cache);
	}

	@Override
	public String getName() {
		return "Linux";
	}

	@Override
	public void uninstall() {

	}

	@Override
	public String toString() {
		return "Linux";
	}

	@Override
	public void getVMs(Collection<JVM> vms) throws Exception {
		String javaHome = System.getenv("JAVA_HOME");
		Map<File, JVM> mvms = new HashMap<>();
		Set<File> jhomes = new HashSet<>();

		if (javaHome != null) {
			File jhome = new File(javaHome).getAbsoluteFile();
			jhomes.add(jhome);
		}
		for (String vmdir : VMDIRS) {
			File jhome = new File(vmdir);
			if (jhome.isDirectory())
				jhomes.add(jhome);
		}

		Stream<String> paths = Stream.of(System.getenv("PATH")
				.split(PATH_SEPARATOR));

		Optional<Path> optJavaPath = paths.map(Paths::get)
				.map(path -> path.resolve("java"))
				.filter(Files::exists)
				.findFirst();

		if (optJavaPath.isPresent()) {
			File javaPath = optJavaPath.get().toFile();

			javaPath = resolveLinks(javaPath);
			File javaParent = javaPath.getParentFile()
					.getParentFile();
			jhomes.add(javaParent);
		}

		for (File jhome : jhomes) {
			if (!jhome.isDirectory())
				continue;
			findVM(jhome, 4, mvms);
		}
	}

	private void findVM(File jhome, int depth,Map<File,JVM> js) throws IOException {
		if (depth <= 0)
			return;

		jhome = resolveLinks(jhome);
		if ( js.containsKey(jhome))
			return;
		
		JVM jvm= getJVM(jhome);
		if ( jvm != null) {
			js.put(jhome,jvm);
			return;
		}
		
		for ( File sub : jhome.listFiles()) {
			if ( sub.isDirectory()) {
				findVM(sub,depth-1,js);
			}
		}
	}

	private File resolveLinks(File p) throws IOException {
		Path path = p.toPath();
		while (Files.isSymbolicLink(path)) {
			path = Files.readSymbolicLink(path);
		}
		return path.toFile();
	}

	@Override
	public JVM getJVM(File vmdir) {
		if (!vmdir.isDirectory()) {
			return null;
		}

		File binDir = new File(vmdir, "bin");
		if (!binDir.isDirectory()) {
			logger.debug("Found a directory {}, but it does not have the expected bin directory", vmdir);
			return null;
		}

		File libDir = new File(vmdir, "lib");
		if (!libDir.isDirectory()) {
			logger.debug("Found a directory {}, but it does not have the expected lib directory", vmdir);
			return null;
		}

		JVM jvm = new JVM();
		jvm.name = vmdir.getName();
		jvm.javahome = vmdir.getAbsolutePath();
		jvm.version = getVersion(vmdir);
		jvm.platformVersion = jvm.version;

		return jvm;
	}

	private String getVersion(File vmdir) {
		File javaExe = new File(vmdir, "bin/java");

		String javaVersionOutput = null;

		try {
			if (javaExe.exists()) {

				ProcessBuilder builder = new ProcessBuilder(javaExe.getAbsolutePath(), "-version");

				try (BufferedReader reader = IO.reader(builder.start()
						.getErrorStream())) {
					javaVersionOutput = reader.lines()
							.collect(Collectors.joining(System.lineSeparator()));
				}

				try (Scanner scanner = new Scanner(javaVersionOutput)) {

					Pattern pattern = Pattern.compile("[1-9][0-9]*((.0)*.[1-9][0-9]*)*");

					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();

						Matcher matcher = pattern.matcher(line);

						if (matcher.find()) {
							return matcher.group();
						}
					}
				}
			}
		} catch (IOException e) {
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Unable to find java version for directory: ");
		sb.append(vmdir.getAbsolutePath());
		sb.append(System.lineSeparator());
		sb.append("\"java -version\" output: ");
		sb.append(System.lineSeparator());
		sb.append(javaVersionOutput);

		throw new NoSuchElementException(sb.toString());
	}

}
