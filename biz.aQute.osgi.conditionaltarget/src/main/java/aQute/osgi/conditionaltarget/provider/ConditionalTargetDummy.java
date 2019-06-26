package aQute.osgi.conditionaltarget.provider;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

import aQute.osgi.conditionaltarget.api.ConditionalTarget;

/**
 * Only here to provide a service capability. Should never be enabled 
 */
@Component(enabled=false)
public class ConditionalTargetDummy implements ConditionalTarget<Object>{

	@Override
	public Iterator<Object> iterator() {
		return null;
	}

	@Override
	public List<Object> getServices() {
		return null;
	}

	@Override
	public Map<ServiceReference<Object>, Object> getServiceReferences() {
		return null;
	}

	@Override
	public Map<String, Object> getAggregateProperties() {
		return null;
	}

}
