package aQute.osgi.conditionaltarget.provider;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.osgi.conditionaltarget.api.ConditionalTarget;

/**
 * Conditional Target addresses the issue that sometimes you need a set of
 * whiteboard services that satisfy some constraint. In DS you can specify the
 * cardinality but there is no way to specify a filter that handles the
 * aggregate of of the properies. I.e. the number of services or the average of
 * a given service property.
 * <p>
 * This class checks the Service Component Runtime for components that have
 * their target set and are looking for a Conditional Target that has a service
 * property `T`. This T is the class the component wants to get a list of. This
 * class then creates a Conditional Target service with the proper properties so
 * the target filter can match.
 *
 */
class ConditionalTargetManager extends BundleTracker<Bundle> implements ListenerHook {
	final static Logger						logger					= LoggerFactory
			.getLogger(ConditionalTargetManager.class);
	final static String						CONDITIONAL_TARGET_NAME	= ConditionalTarget.class.getName();
	final static String[]					SPECIAL					= new String[] { "T", "#" };

	final ServiceComponentRuntime			scr;
	final Map<String, ReferenceHandler<?>>	targets					= new HashMap<>();
	final Trigger							trigger					= new Trigger(this::syncSCR, 500);

	ConditionalTargetManager(BundleContext context, ServiceComponentRuntime scr) {
		super(context, Bundle.ACTIVE + Bundle.STARTING, null);
		this.scr = scr;
		trigger.trigger();
		logger.info("Creating target manager");
	}

	/**
	 * Synchronize SCR data with our current view of the set of bundles
	 */

	@SuppressWarnings({ "rawtypes", "unchecked" })
	synchronized void syncSCR() {
		Map<String, Class<?>> newer = new HashMap<>();
		Set<String> older = targets.keySet();

		for (ComponentDescriptionDTO cd : scr.getComponentDescriptionDTOs()) {
			Map<String, Class<?>> references = new HashMap<>();
			for (ReferenceDTO r : cd.references) {
				if (r.interfaceName.equals(CONDITIONAL_TARGET_NAME)) {
					try {
						Type T = getT(cd, r);
						references.put(r.name, (Class<?>) T);
					} catch (Exception e) {
						logger.error("Fail to get the type of the Conditional Target {}", e);
					}
				}
			}
			if (references.isEmpty())
				continue;

			logger.info("References for {}", cd.name);

			for (ComponentConfigurationDTO cc : scr.getComponentConfigurationDTOs(cd)) {
				for (SatisfiedReferenceDTO r : cc.satisfiedReferences) {
					Class<?> T = references.get(r.name);
					if (T != null && r.target != null)
						newer.put(r.target, T);
				}
				for (UnsatisfiedReferenceDTO r : cc.unsatisfiedReferences) {
					Class<?> T = references.get(r.name);
					if (T != null)
						newer.put(r.target, T);
				}
			}
		}

		Set<String> toDelete = new HashSet<>(older);
		Set<String> toAdd = new HashSet<>(newer.keySet());

		toDelete.removeAll(newer.keySet());
		toAdd.removeAll(older);

		for (String a : toAdd)
			try {
				targets.put(a, new ReferenceHandler<Object>(a, (Class) newer.get(a), context));
			} catch (Exception e) {
				logger.error("Fail to create a ConditionalTargetImpl {} for filter {}", e, a);
			}

		for (String d : toDelete) {
			targets.remove(d).close();
		}
	}

	private Type getT(ComponentDescriptionDTO cd, ReferenceDTO r) throws ClassNotFoundException, NoSuchFieldException {
		String implementationClass = cd.implementationClass;
		Bundle b = context.getBundle(cd.bundle.id);
		Class<?> loadClass = b.loadClass(implementationClass);
		Field field = loadClass.getDeclaredField(r.name);
		if (field.getGenericType() instanceof ParameterizedType) {
			ParameterizedType genericType = (ParameterizedType) field.getGenericType();
			Type T = genericType.getActualTypeArguments()[0];
			return T;
		} else {
			logger.error("Use of ConditionalTarget without generic parameter. Component={}, Field is {}", cd.name, r.name);
			return Object.class;
		}
	}

	@Override
	public void added(Collection<ListenerInfo> listeners) {
		triggerIfConditionalTargetRequested(listeners);
	}

	@Override
	public void removed(Collection<ListenerInfo> listeners) {
		triggerIfConditionalTargetRequested(listeners);
	}

	private void triggerIfConditionalTargetRequested(Collection<ListenerInfo> listeners) {
		for (ListenerInfo info : listeners) {
			if (isConditionalTarget(info)) {
				trigger.trigger();
			}
		}
	}

	private boolean isConditionalTarget(ListenerInfo info) {
		if (info.getBundleContext().getBundle().getBundleId() == 0L)
			return false;

		String filter = info.getFilter();
		return filter != null
				&& filter.contains("(objectClass=" + ConditionalTarget.class.getName() + ")");
	}

	@Override
	public void close() {
		targets.values().forEach(ReferenceHandler::close);
	}

}
