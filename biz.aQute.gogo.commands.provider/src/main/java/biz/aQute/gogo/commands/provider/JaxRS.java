package biz.aQute.gogo.commands.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.BundleContext;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.util.tracker.ServiceTracker;

import biz.aQute.gogo.commands.dtoformatter.DTOFormatter;

public class JaxRS {

	final ServiceTracker<JaxrsServiceRuntime, JaxrsServiceRuntime>	tracker;
	final BundleContext												context;

	public JaxRS(BundleContext context, DTOFormatter formatter) {
		this.context = context;
		dtos(formatter);
		// dtos(formatter);
		tracker = new ServiceTracker<>(context, JaxrsServiceRuntime.class, null);
		tracker.open();
	}

	void dtos(DTOFormatter formatter) {
		formatter.build(RuntimeDTO.class)
			.inspect()
			.fields("*")
			.line()
			.part()
			.as(dto -> String.format("[%s] ", dto.serviceDTO.id));

		formatter.build(ApplicationDTO.class)
			.inspect()
			.fields("*")
			.line()
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(ExtensionDTO.class)
			.inspect()
			.fields("*")
			.line()
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(FailedResourceDTO.class)
			.inspect()
			.fields("*")
			.line()
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(FailedExtensionDTO.class)
			.inspect()
			.fields("*")
			.line()
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(FailedApplicationDTO.class)
			.inspect()
			.fields("*")
			.line()
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(ResourceDTO.class)
			.inspect()
			.fields("*")
			.line()
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(ResourceMethodInfoDTO.class)
			.inspect()
			.fields("*")
			.line()
			.part()
			.as(dto -> String.format("[%s] %s", dto.method, dto.path));

	}

	private List<JaxrsServiceRuntime> serviceRuntimes() {

		return Arrays.asList((JaxrsServiceRuntime[]) tracker.getServices());
	}

	private JaxrsServiceRuntime serviceRuntime() {

		return tracker.getService();
	}

	@Descriptor("Show the RuntimeDTO of the HttpServiceRuntime")
	public List<RuntimeDTO> jaxrsruntimes() throws InterruptedException {

		return serviceRuntimes().stream()
			.map(JaxrsServiceRuntime::getRuntimeDTO)
			.collect(Collectors.toList());
	}

	@Descriptor("Show the RuntimeDTO of the JaxrsServiceRuntime")
	public RuntimeDTO jaxrsruntime(@Descriptor("service.id")
	@Parameter(absentValue = "-1", names = "-s")
	long service_id) throws InterruptedException {

		return runtime(service_id).map(JaxrsServiceRuntime::getRuntimeDTO)
			.orElse(null);
	}

	private Optional<JaxrsServiceRuntime> runtime(long service_id) {

		if (service_id < 0) {
			return Optional.ofNullable(serviceRuntime());
		}
		return serviceRuntimes().stream()
			.filter(runtime -> runtimeHasServiceId(runtime, service_id))
			.findAny();
	}

	private static boolean runtimeHasServiceId(JaxrsServiceRuntime runtime, long service_id) {

		return service_id == runtime.getRuntimeDTO().serviceDTO.id;
	}

}
