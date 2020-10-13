package biz.aQute.osgi.spy.runpath;

import java.security.Permission;
import java.util.Hashtable;
import java.util.TreeSet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Spy implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		SecurityManager sm = new SecurityManager() {
			final TreeSet<String>			permissions = new TreeSet<>();

			@Override
			public void checkPermission(Permission perm) {
				permissions.add( perm.toString());
			}
		};

		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put( "osgi.command.function", new String[] {"permissions"});
		properties.put( "osgi.command.scope", "spy");
		context.registerService(SecurityManager.class, sm, properties);

		System.setSecurityManager(sm);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
