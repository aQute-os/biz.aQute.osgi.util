package aQute.osgi.conditionaltarget.provider;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.osgi.conditionaltarget.api.ConditionalTarget;

/**
 * This class monitors bundles that are 'looking' for Conditional target
 * services. If it finds such a bundle through the service hooks, it checks the
 * Service Component Runtime for components that have their target set and are
 * looking for a Conditional Target.
 * <p>
 * Whenever it finds a bundle looking, it triggers a timer. If this timer
 * expires (it can be extended by new triggers) the SCR state is compared with
 * the previous state. For each newly discovered target (the filter string + T)
 * a {@link CTTargetHandler} is created. This {@link CTTargetHandler} is
 * registered as a {@link ConditionalTarget} service. It uses a
 * {@link ServiceFactory} and creates a bundle scoped object
 * {@link CTBundleInstance} for each bundle. (This is needed because objects
 * should be checked out per bundle.) The {@link CTTargetHandler} tracks the
 * {@link ServiceReference}s and keeps the {@link CTBundleInstance}s current
 * with the selected {@link ServiceReference}s. It also calculates the aggregate
 * properties. (Clearly this is tricky from a synchronization POV.
 */
class CTServer implements ListenerHook {
	final static String						FILTER_TYPE				= "(objectClass="
			+ ConditionalTarget.class.getName() + ")";
	final static Logger						logger					= LoggerFactory
			.getLogger(CTServer.class);
	final static String						CONDITIONAL_TARGET_NAME	= ConditionalTarget.class.getName();
	final static String[]					SPECIAL					= new String[] { "T", "#" };

	final BundleContext						context;
	final ServiceComponentRuntime			scr;
	final Map<String, CTTargetHandler<?>>	targets					= new HashMap<>();
	final Trigger							trigger					= new Trigger(this::syncSCR, 500);
	final AtomicBoolean						inside					= new AtomicBoolean(false);
	final AtomicBoolean						closed					= new AtomicBoolean(false);

	/*
	 *
	 */
	CTServer(BundleContext context, ServiceComponentRuntime scr) {
		this.context = context;
		this.scr = scr;
		trigger.trigger(); // first event
		logger.info("Created target manager");
	}

	/*
	 * Synchronize SCR data with our current view of the set of bundles this
	 * code may not be called concurrently!
	 */

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void syncSCR() {

		// we only can run this one at a time. The caller (see Trigger) should make sure of
		// this but we verify it

		assert inside.getAndSet(true) == false;

		try {
			Map<String, Class<?>> newer = closed.get() ? Collections.emptyMap() : currentRequestedTargets();

			Set<String> toAdd = new HashSet<>(newer.keySet());
			Set<String> older = targets.keySet();
			Set<String> toDelete = new HashSet<>(older);

			toDelete.removeAll(newer.keySet());
			toAdd.removeAll(older);

			for (String a : toAdd)
				try {
					CTTargetHandler<Object> refHandler = new CTTargetHandler<Object>(a, (Class) newer.get(a),
							context);
					targets.put(a, refHandler);
				} catch (Exception e) {
					logger.error("Fail to create a ConditionalTargetImpl {} for filter {}", e, a);
				}

			for (String d : toDelete) {
				targets.remove(d).close();
			}
		} catch (Exception e) {
			logger.error("Failed to sync {}", e);
		} finally {
			inside.set(false);
		}
	}

	private Map<String, Class<?>> currentRequestedTargets() {
		Map<String, Class<?>> requestedTargets = new HashMap<>();

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
						requestedTargets.put(r.target, T);
				}
				for (UnsatisfiedReferenceDTO r : cc.unsatisfiedReferences) {
					Class<?> T = references.get(r.name);
					if (T != null)
						requestedTargets.put(r.target, T);
				}
			}
		}
		return requestedTargets;
	}

	/*
	 * Find out the generic type parameter of the Conditional Target
	 */
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
			logger.error("Use of ConditionalTarget without generic parameter. Component={}, Field is {}", cd.name,
					r.name);
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
				&& filter.contains(FILTER_TYPE);
	}

	public void close() {
		if (closed.getAndSet(true))
			return;
		trigger.immediate();
	}

}
