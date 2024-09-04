package biz.aQute.gogo.commands.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.BundleContext;
import org.osgi.service.jakartars.runtime.JakartarsServiceRuntime;
import org.osgi.service.jakartars.runtime.dto.ApplicationDTO;
import org.osgi.service.jakartars.runtime.dto.DTOConstants;
import org.osgi.service.jakartars.runtime.dto.ExtensionDTO;
import org.osgi.service.jakartars.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jakartars.runtime.dto.FailedExtensionDTO;
import org.osgi.service.jakartars.runtime.dto.FailedResourceDTO;
import org.osgi.service.jakartars.runtime.dto.ResourceDTO;
import org.osgi.service.jakartars.runtime.dto.ResourceMethodInfoDTO;
import org.osgi.service.jakartars.runtime.dto.RuntimeDTO;
import org.osgi.util.tracker.ServiceTracker;

import biz.aQute.gogo.commands.dtoformatter.DTOFormatter;

public class JakartaRS {

	final ServiceTracker<JakartarsServiceRuntime, JakartarsServiceRuntime>	tracker;
	final BundleContext												context;

	public JakartaRS(BundleContext context, DTOFormatter formatter) {
		this.context = context;
		dtos(formatter);
		// dtos(formatter);
		tracker = new ServiceTracker<>(context, JakartarsServiceRuntime.class, null);
		tracker.open();
	}

	void dtos(DTOFormatter formatter) {
		formatter.build(RuntimeDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s] ", dto.serviceDTO.id));

		formatter.build(FailedApplicationDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.format("failureReason", f -> failedReason(f.failureReason))
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(ApplicationDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(FailedExtensionDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.format("failureReason", f -> failedReason(f.failureReason))
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(ExtensionDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(FailedResourceDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.format("failureReason", f -> failedReason(f.failureReason))
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(ResourceDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(ResourceMethodInfoDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s] %s", dto.method, dto.path));

	}

	private List<JakartarsServiceRuntime> serviceRuntimes() {

		return Arrays.asList((JakartarsServiceRuntime[]) tracker.getServices());
	}

	private JakartarsServiceRuntime serviceRuntime() {

		return tracker.getService();
	}

	@Descriptor("Show the RuntimeDTO of the JakartarsServiceRuntime")
	public List<org.osgi.service.jakartars.runtime.dto.RuntimeDTO> jakartarsruntimes() throws InterruptedException {

		return serviceRuntimes().stream()
			.map(JakartarsServiceRuntime::getRuntimeDTO)
			.collect(Collectors.toList());
	}

	@Descriptor("Show the RuntimeDTO of the JakartarsServiceRuntime")
	public RuntimeDTO jakartarsruntime(@Descriptor("service.id")
	@Parameter(absentValue = "-1", names = "-s")
	long service_id) throws InterruptedException {

		return runtime(service_id).map(JakartarsServiceRuntime::getRuntimeDTO)
			.orElse(null);
	}

	private Optional<JakartarsServiceRuntime> runtime(long service_id) {

		if (service_id < 0) {
			return Optional.ofNullable(serviceRuntime());
		}
		return serviceRuntimes().stream()
			.filter(runtime -> runtimeHasServiceId(runtime, service_id))
			.findAny();
	}

	private static boolean runtimeHasServiceId(JakartarsServiceRuntime runtime, long service_id) {

		return service_id == runtime.getRuntimeDTO().serviceDTO.id;
	}

	private String failedReason(int failureReason) {

		switch (failureReason) {
			case DTOConstants.FAILURE_REASON_UNKNOWN :
				return "UNKNOWN";
			case DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE :
				return "FAILURE_REASON_UNKNOWN";
			case DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE :
				return "SERVICE_NOT_GETTABLE";
			case DTOConstants.FAILURE_REASON_VALIDATION_FAILED :
				return "VALIDATION_FAILED";
			case DTOConstants.FAILURE_REASON_REQUIRED_EXTENSIONS_UNAVAILABLE :
				return "REQUIRED_EXTENSIONS_UNAVAILABLE";
			case DTOConstants.FAILURE_REASON_DUPLICATE_NAME :
				return "DUPLICATE_NAME";
			case DTOConstants.FAILURE_REASON_REQUIRED_APPLICATION_UNAVAILABLE :
				return "REQUIRED_APPLICATION_UNAVAILABLE";
			default :
				return failureReason + "";
		}
	}

}
