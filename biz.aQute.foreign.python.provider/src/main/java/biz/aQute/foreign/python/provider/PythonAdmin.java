package biz.aQute.foreign.python.provider;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import biz.aQute.foreign.python.api.ProvideForeignPython;
import biz.aQute.foreign.python.configuration.Configuration;

@ProvideForeignPython
@GogoCommand(scope = "python", function="python")
@Component(immediate = true, service = Object.class, name = Configuration.PID)
public class PythonAdmin implements BundleTrackerCustomizer<PythonApp> {
	final BundleTracker<PythonApp>	bundles;
	final String					python;
	final CommandProcessor			gogo;
	final long restartDelay;

	@Activate
	public PythonAdmin(BundleContext context, Configuration config, @Reference CommandProcessor cp) {
		this.gogo = cp;
		this.python = config.python();
		this.restartDelay = Math.max(1000 , config.restartDelay());
		this.bundles = new BundleTracker<>(context, Bundle.ACTIVE + Bundle.STARTING, this);
		this.bundles.open();
	}

	@Override
	public PythonApp addingBundle(Bundle bundle, BundleEvent event) {
		URL entry = bundle.getEntry("python/app.py");
		if (entry == null)
			return null;
		try {
			PythonApp app = new PythonApp(bundle, python,gogo, restartDelay);
			app.open();
			return app;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, PythonApp object) {

	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, PythonApp app) {
		try {
			app.close();
		} catch (InterruptedException e) {
			// ignore
		}
	}

	@Descriptor("List the running Python applications and their status")
	public List<PythonApp> python() {
		return new ArrayList<>(bundles.getTracked().values());
	}

}
