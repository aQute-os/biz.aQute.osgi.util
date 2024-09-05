package biz.aQute.gogo.commands.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.FailedPreprocessorDTO;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.PreprocessorDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;
import org.osgi.util.tracker.ServiceTracker;

import biz.aQute.gogo.commands.dtoformatter.DTOFormatter;

public class HTTP {

	final ServiceTracker<HttpServiceRuntime, HttpServiceRuntime>	tracker;
	final BundleContext												context;

	public HTTP(BundleContext context, DTOFormatter formatter) {
		this.context = context;
		dtos(formatter);
		tracker = new ServiceTracker<>(context, HttpServiceRuntime.class, null);
		tracker.open();
	}

	void dtos(DTOFormatter formatter) {
		formatter.build(RuntimeDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s] port: %s", dto.serviceDTO.id, port(dto)));

		formatter.build(FailedServletContextDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.format("failureReason", f -> failedReason(f.failureReason))
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(ServletContextDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(FailedPreprocessorDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.format("failureReason", f -> failedReason(f.failureReason))
			.part()
			.as(dto -> String.format("[%s]", dto.serviceId));

		formatter.build(PreprocessorDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s] ", dto.serviceId));

		formatter.build(FailedServletDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.format("failureReason", f -> failedReason(f.failureReason))
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(ServletDTO.class)
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
			.as(dto -> String.format("[%s]", dto.serviceId));

		formatter.build(ResourceDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s]", dto.serviceId));

		formatter.build(FailedFilterDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.format("failureReason", f -> failedReason(f.failureReason))
			.part()
			.as(dto -> String.format("[%s]", dto.serviceId));

		formatter.build(FilterDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(FailedErrorPageDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.format("failureReason", f -> failedReason(f.failureReason))
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(ErrorPageDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s] %s", dto.serviceId, dto.name));

		formatter.build(FailedListenerDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.format("failureReason", f -> failedReason(f.failureReason))
			.part()
			.as(dto -> String.format("[%s]", dto.serviceId));

		formatter.build(ListenerDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s]", dto.serviceId));

		formatter.build(RequestInfoDTO.class)
			.inspect()
			.fields("*")
			.line()
			.fields("*")
			.part()
			.as(dto -> String.format("[%s] ", dto.path));

	}

	private List<HttpServiceRuntime> serviceRuntimes() {

		List<HttpServiceRuntime> list = new ArrayList<>();

		if (!tracker.isEmpty()) {
			for (Object httpServiceRuntimeO : tracker.getServices()) {
				HttpServiceRuntime h = (HttpServiceRuntime) httpServiceRuntimeO;
				list.add(h);
			}
		}

		return list;
	}

	private HttpServiceRuntime serviceRuntime() {

		return tracker.getService();
	}

	@Descriptor("Show the RuntimeDTO of the HttpServiceRuntime")
	public List<RuntimeDTO> httpruntimes() throws InterruptedException {

		return serviceRuntimes().stream()
			.map(HttpServiceRuntime::getRuntimeDTO)
			.collect(Collectors.toList());
	}

	@Descriptor("Show the RuntimeDTO of the HttpServiceRuntime")
	public RuntimeDTO httpruntime(@Descriptor("Port")
	@Parameter(absentValue = "", names = "-p")
	String port) throws InterruptedException {

		return runtime(port).map(HttpServiceRuntime::getRuntimeDTO)
			.orElse(null);
	}

	@Descriptor("Show the RequestInfoDTO of the HttpServiceRuntime")
	public RequestInfoDTO requestInfo(@Descriptor("Port")
	@Parameter(absentValue = "", names = "-p")
	String port, @Descriptor("Path")
	@Parameter(absentValue = "", names = "-pa")
	String path) throws InterruptedException {

		return runtime(port).map(runtime -> runtime.calculateRequestInfoDTO(path))
			.orElse(null);
	}

	private Optional<HttpServiceRuntime> runtime(String port) {

		if (port.isEmpty()) {
			return Optional.ofNullable(serviceRuntime());
		}
		return serviceRuntimes().stream()
			.filter(runtime -> runtimeHasPort(runtime, port))
			.findAny();
	}

	private static boolean runtimeHasPort(HttpServiceRuntime runtime, String port) {
		return port == port(runtime);
	}

	private static String port(HttpServiceRuntime runtime) {
		RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
		return port(runtimeDTO);
	}

	private static String port(RuntimeDTO runtimeDTO) {
		Map<String, Object> map = runtimeDTO.serviceDTO.properties;
		String p = map.get("org.osgi.service.http.port")
			.toString();
		return p;
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
			case DTOConstants.FAILURE_REASON_SERVICE_IN_USE :
				return "SERVICE_IN_USE";
			case DTOConstants.FAILURE_REASON_SERVLET_WRITE_TO_LOCATION_DENIED :
				return "SERVLET_WRITE_TO_LOCATION_DENIED";
			case DTOConstants.FAILURE_REASON_WHITEBOARD_WRITE_TO_DEFAULT_DENIED :
				return "WHITEBOARD_WRITE_TO_DEFAULT_DENIED";
			case DTOConstants.FAILURE_REASON_SERVLET_READ_FROM_DEFAULT_DENIED :
				return "SERVLET_READ_FROM_DEFAULT_DENIED";
			case DTOConstants.FAILURE_REASON_WHITEBOARD_WRITE_TO_LOCATION_DENIED :
				return "WHITEBOARD_WRITE_TO_LOCATION_DENIED";
			default :
				return failureReason + "";
		}
	}

}
