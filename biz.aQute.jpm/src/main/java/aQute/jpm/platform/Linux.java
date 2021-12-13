package aQute.jpm.platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.jpm.api.JVM;

class Linux extends Unix {
	private final static Logger		logger	= LoggerFactory.getLogger(Linux.class);
	// /etc/alternatives/java
	final static String[]		VMDIRS					= { "/usr/lib/jvm", "/usr/java/", "/usr/local/java/jdk",
			"/usr/lib64/jvm" };

	private static final String	PATH_SEPARATOR			= Pattern.quote(File.pathSeparator);

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
		Map<File, JVM> mvms = new HashMap<>();
		Set<File> jhomes = new HashSet<>();

		String javaHome = System.getenv("JAVA_HOME");
		if (javaHome != null) {
			File jhome = new File(javaHome).getAbsoluteFile();
			logger.debug("JAVA_HOME dir ", javaHome);
			jhomes.add(jhome);
		}
		
		for (String vmdir : VMDIRS) {
			File jhome = new File(vmdir);
			if (jhome.isDirectory()) {
				logger.debug("Typical vm dir ", jhome);
				for ( File f : jhome.listFiles()) {
					if ( f.isDirectory()) {
						logger.debug("Candidate ", f);
						jhomes.add(f);
					}
				}
			}
		}

		Stream<String> paths = Stream.of(System.getenv("PATH")
				.split(PATH_SEPARATOR));

		Optional<Path> optJavaPath = paths.map(Paths::get)
				.map(path -> path.resolve("java"))
				.filter(Files::exists)
				.findFirst();

		if (optJavaPath.isPresent()) {
			File javaPath = optJavaPath.get().toFile();
			logger.debug("Found  Java in path {}", javaPath);

			javaPath = resolveLinks(javaPath);
			File javaParent = javaPath.getParentFile() //bin
					.getParentFile(); // javahome
			logger.debug("Candidate from  Java in path {}", javaParent);
			jhomes.add(javaParent);
		}

		for (File jhome : jhomes) {
			
			jhome = resolveLinks(jhome);
			logger.debug("Resolved VM dir candidate {}", jhome);
			
			if (!jhome.isDirectory()) {
				logger.debug("Resolved VM dir candidate {}", jhome);
				continue;
			}
			
			if ( mvms.containsKey(jhome))
				return;
			
			JVM jvm= getJVM(jhome);
			if ( jvm != null) {
				logger.debug("VM found {}", jvm);
				mvms.put(jhome,jvm);
			}
		}
		
		vms.addAll(mvms.values());
	}

	private File resolveLinks(File p) throws IOException {
		Path path = p.toPath();
		while (Files.isSymbolicLink(path)) {
			path = Files.readSymbolicLink(path);
		}
		return path.toFile();
	}
}
