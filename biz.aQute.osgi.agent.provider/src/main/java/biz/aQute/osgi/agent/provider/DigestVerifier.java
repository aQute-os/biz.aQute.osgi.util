package biz.aQute.osgi.agent.provider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.osgi.framework.Bundle;

/**
 * Abstracts the fact if a bundle is the same as the one wanted by storing a
 * digest in the bundle file area.
 */
public class DigestVerifier {
	private static final String	AGENT	= ".agent";
	private static final String	DIGEST	= "digest";

	public boolean verifyDigest(Bundle bundle, byte[] expected) throws IOException {
		File f = bundle.getDataFile(AGENT);
		if (!f.isDirectory())
			return false;

		f = new File(f, DIGEST);
		if (!f.isFile())
			return false;

		byte[] ondisk = Files.readAllBytes(f.toPath());

		return Arrays.equals(ondisk, expected);
	}

	public void updateDigest(Bundle bundle, byte[] digest) throws IOException {
		File f = bundle.getDataFile(AGENT);
		if (!f.isDirectory()) {
			if (!f.mkdirs()) {
				throw new IOException("Cannot create directory for writing digest " + f);
			}
		}

		f = new File(f, DIGEST);

		Files.write(f.toPath(), digest, StandardOpenOption.CREATE);
	}
}
