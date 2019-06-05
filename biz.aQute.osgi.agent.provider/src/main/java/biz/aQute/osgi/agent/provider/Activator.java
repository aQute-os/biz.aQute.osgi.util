package biz.aQute.osgi.agent.provider;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

import biz.aQute.osgi.agent.api.UpdateAgent;

/**
 * An old fashioned activator that starts the domain objects with all the
 * parameters from the environment it needs.
 */
@SuppressWarnings("deprecation")
public class Activator implements BundleActivator {
	private UpdateAgentImpl													agent;
	private ServiceTracker<PackageAdmin, ServiceRegistration<UpdateAgent>>	tracker;
	private Executor														executor	= Executors
			.newScheduledThreadPool(4);

	@Override
	public void start(BundleContext context) throws Exception {
		tracker = getTracker(context);
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		tracker.close();
	}

	/*
	 * Awfully ugly method to track the Package Admin. This is a bit overdone
	 * since the Package Admin is registered by the framework today. However, it
	 * is deprecated and then some frameworks will provide this is in a
	 * 'compatibility' bundle one day. So to prevent from crashing that one day
	 * we do this ugly service tracking that DS made unnecessary ...
	 */
	private ServiceTracker<PackageAdmin, ServiceRegistration<UpdateAgent>> getTracker(BundleContext context) {

		return new ServiceTracker<PackageAdmin, ServiceRegistration<UpdateAgent>>(context, PackageAdmin.class,
				null) {

			@Override
			public ServiceRegistration<UpdateAgent> addingService(ServiceReference<PackageAdmin> reference) {
				assert agent == null;
				try {
					PackageAdmin pa = context.getService(reference);
					agent = new UpdateAgentImpl(context, executor,pa, new Downloader(executor), new DigestVerifier());
					return (ServiceRegistration<UpdateAgent>) context.registerService(UpdateAgent.class, agent, null);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}

			@Override
			public void removedService(ServiceReference<PackageAdmin> reference,
					ServiceRegistration<UpdateAgent> registration) {
				try {
					registration.unregister();
					super.removedService(reference, registration);
					UpdateAgentImpl tmp = agent;
					agent = null;
					tmp.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

	}

}
