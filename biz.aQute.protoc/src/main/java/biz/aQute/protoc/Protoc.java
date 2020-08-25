package biz.aQute.protoc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.command.Command;

public class Protoc {

	public static void main(String args[]) throws Exception {
		File f = IO.getFile("~/.bnd/cache/protoc");
		long modified = getModified();
		if (!f.isFile() || f.lastModified() < modified) {
			String name;
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("windows"))
				name = "/exe/protoc-windows-x86_64";
			else if (osName.startsWith("mac") || osName.startsWith("darwin") || osName.startsWith("osx")) {
				name = "/exe/protoc-osx-x86_64";
			} else if (osName.contains("linux"))
				name = "/exe/protoc-linux-x86_64";
			else
				throw new IllegalArgumentException("Have find executable for " + osName);

			URL resource = Protoc.class.getResource(name);
			if (resource == null) {
				throw new IllegalArgumentException("Corrupt jar, not found " + name);
			}
			IO.copy(resource, f);
			f.setLastModified(modified);
			Files.setPosixFilePermissions(f.toPath(),
					EnumSet.of(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
		}
		Command cmd = new Command();
		cmd.add(f.getAbsolutePath());
		cmd.add(args);
		int execute = cmd.execute(System.in, System.out, System.err);
		if (execute != 0)
			System.exit(execute);
	}

	private static long getModified() throws NumberFormatException, IOException {
		String timestamp = IO.collect(Protoc.class.getResource("timestamp.txt"));
		if ( timestamp == null) {
			return 0L;
		}
		
		return Long.parseLong(Strings.trim(timestamp));
	}
}
