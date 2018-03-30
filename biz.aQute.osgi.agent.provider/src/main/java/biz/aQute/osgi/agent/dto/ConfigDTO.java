package biz.aQute.osgi.agent.dto;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.osgi.dto.DTO;

public class ConfigDTO extends DTO {
	public String				configurationHash;
	public URI					repositoryUrl;
	public List<BundleRefDTO>	bundles = Collections.emptyList();

}
