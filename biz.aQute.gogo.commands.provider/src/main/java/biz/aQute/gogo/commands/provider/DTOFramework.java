package biz.aQute.gogo.commands.provider;

import java.util.List;

import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

import biz.aQute.gogo.commands.dtoformatter.DTOFormatter;

public class DTOFramework {

	private BundleContext context;

	@SuppressWarnings("deprecation")
	public DTOFramework(BundleContext context, DTOFormatter formatter) {
		this.context = context;
		dtos(formatter);
	}

	void dtos(DTOFormatter formatter) {
		formatter.build(FrameworkDTO.class)
			.inspect()
			.fields("*")
			.line()
			.part();

	}


	@Descriptor(value = "delivers the FrameworkDTO")
	public FrameworkDTO frameworkDTO() {
		final Bundle bundle = context.getBundle(0);
		final FrameworkDTO frameworkDTO = bundle.adapt(FrameworkDTO.class);
		return frameworkDTO;
	}

	@Descriptor(value = "delivers the BundleDTOs")
	public List<BundleDTO> bundleDTO() {
		return frameworkDTO().bundles;
	}

	@Descriptor(value = "delivers the BundleDTO by id")
	public BundleDTO bundleDTO(long id) {
		return bundleDTO().stream()
			.filter(dto -> dto.id == id)
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException(""));
	}

	@Descriptor(value = "delivers the ServiceReferenceDTOs")
	public List<ServiceReferenceDTO> serviceReferenceDTO() {
		return frameworkDTO().services;
	}

	@Descriptor(value = "delivers the ServiceReferenceDTO by id")
	public ServiceReferenceDTO serviceReferenceDTO(long id) {
		return serviceReferenceDTO().stream()
			.filter(dto -> dto.id == id)
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException(""));
	}
}
