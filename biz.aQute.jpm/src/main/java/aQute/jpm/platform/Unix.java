package aQute.jpm.platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.jpm.api.CommandData;
import aQute.jpm.api.JVM;
import aQute.lib.io.IO;

public abstract class Unix extends PlatformImpl {
	private final static Logger	logger		= LoggerFactory.getLogger(Unix.class);

	public static String		JPM_GLOBAL	= "/var/jpm";

	Unix(File cache) {
		super(cache);
	}

	@Override
	public String createCommand(CommandData data, Map<String, String> map, boolean force, JVM vm, File binDir,
		String... extra) throws Exception {

		File bin = new File(binDir, data.name);
		data.bin = bin.getAbsolutePath();

		if (bin.exists())
			if (!force)
				return "Command already exists " + data.bin;
			else
				delete(bin);

		String java = priority( //
			data.java, //
			vm != null ? (data.windows ? vm.javaw() : vm.java()).getAbsolutePath() : null, //
			data.windows ? "javaw" : "java" //
		);

		try (Formatter frm = new Formatter()) {
			frm.format("#!/bin/sh\n");
			frm.format("exec");

			frm.format(" %s", java);

			frm.format(" -Dpid=$$");

			if (data.jvmArgs != null) {
				frm.format(" %s", data.jvmArgs);
			}

			frm.format(" -cp");

			assert !data.dependencies.isEmpty();

			String del = " ";
			for (String dep : data.dependencies) {
				frm.format("%s%s", del, dep);
				del = ":";
			}

			frm.format(" %s \"$@\"\n", data.main);

			String script = frm.toString();
			IO.store(script, bin);

			makeExecutable(bin);
			logger.debug(script);
		}

		return null;
	}

	private void makeExecutable(File f) throws IOException {
		if (f.isFile()) {
			Set<PosixFilePermission> attrs = new HashSet<>();
			attrs.add(PosixFilePermission.OWNER_EXECUTE);
			attrs.add(PosixFilePermission.OWNER_READ);
			attrs.add(PosixFilePermission.OTHERS_READ);
			attrs.add(PosixFilePermission.OTHERS_EXECUTE);
			attrs.add(PosixFilePermission.GROUP_READ);
			attrs.add(PosixFilePermission.GROUP_EXECUTE);
			Files.setPosixFilePermissions(f.toPath(), attrs);
		}
	}

	private void delete(File f) throws IOException {
		if (f.isFile()) {
			Set<PosixFilePermission> attrs = new HashSet<>();
			attrs.add(PosixFilePermission.OWNER_WRITE);
			Files.setPosixFilePermissions(f.toPath(), attrs);
			IO.delete(f);
		}
	}

	@Override
	public void deleteCommand(CommandData data) throws Exception {
		File executable = new File(data.bin);
		IO.deleteWithException(executable);
	}

	@Override
	public void report(Formatter out) {
		out.format("Name     \t%s\n", getName());
	}
}
