package biz.aQute.web.server.provider;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.aQute.servlet.api.ConditionalServlet;
import biz.aQute.web.server.cache.Cache;
import biz.aQute.web.server.cache.CacheFile;
import biz.aQute.web.server.config.WebServerConfig;
import biz.aQute.web.server.exceptions.ExceptionHandler;

/**
 * Maps any "static/" directory to 
 * @author aqute
 *
 */
@Component(service = {
	ConditionalServlet.class
}, immediate = true, property = {
	"service.ranking:Integer=1002", "name=" + BundleMixinServer.NAME, "addTrailingSlash=true"
}, name = BundleMixinServer.NAME, configurationPid = BundleMixinServer.NAME, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class BundleMixinServer implements ConditionalServlet {

	public static final String	NAME	= "biz.aQute.web.support.mixin";
	private final static Logger	log		= LoggerFactory.getLogger(BundleMixinServer.class);

	WebServerConfig				config;
	BundleTracker<?>			tracker;

	@Reference
	Cache						cache;
	private ResponseWriter		writer;
	private ExceptionHandler	exceptionHandler;

	@Activate
	void activate(WebServerConfig config, BundleContext context) throws Exception {
		this.config = config;
		writer = new ResponseWriter(config);
		exceptionHandler = new ExceptionHandler(config.addTrailingSlash(), log);

		tracker = new BundleTracker<Bundle>(context, Bundle.ACTIVE | Bundle.STARTING, null) {
			@Override
			public Bundle addingBundle(Bundle bundle, BundleEvent event) {
				if (bundle.getEntryPaths("static/") != null)
					return bundle;
				return null;
			}
		};
		tracker.open();
	}

	@Override
	public boolean doConditionalService(HttpServletRequest rq, HttpServletResponse rsp) throws Exception {
		try {
			String path = rq.getRequestURI();
			if (path != null && path.startsWith("/"))
				path = path.substring(1);

			CacheFile c = cache.get(path);
			if (c == null || c.isExpired())
				c = findBundle(path);
			if (c == null)
				return false;

			cache.put(path, c);
			writer.writeResponse(rq, rsp, c);
		} catch (Exception e) {
			exceptionHandler.handle(rq, rsp, e);
		}

		return true;
	}

	CacheFile findBundle(String path) throws Exception {
		Bundle[] bundles = tracker.getBundles();
		if (bundles != null) {
			for (Bundle b : bundles) {
				URL url = cache.internalUrlOf(b, path);
				CacheFile c = cache.getFromBundle(b, url, path);
				if (c != null)
					return c;
			}
		}
		return null;
	}

	@Deactivate
	void deactivate() {
		tracker.close();
	}

}
