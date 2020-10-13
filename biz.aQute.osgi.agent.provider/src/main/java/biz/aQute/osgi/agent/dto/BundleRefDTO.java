package biz.aQute.osgi.agent.dto;

import java.net.URI;
import java.util.Map;

import org.osgi.dto.DTO;

public class BundleRefDTO extends DTO {
	public String location;
	public URI path;
	public byte[] digest;

	public String bsn;
	public String version;
	public Map<String,byte[]> bundleHashes;
}
