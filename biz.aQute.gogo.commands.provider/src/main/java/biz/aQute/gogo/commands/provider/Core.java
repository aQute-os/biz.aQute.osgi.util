package biz.aQute.gogo.commands.provider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.ServiceTracker;

import aQute.libg.glob.Glob;
import biz.aQute.gogo.commands.dtoformatter.DTOFormatter;

public class Core {

	final BundleContext																								context;
	@SuppressWarnings("deprecation")
	final ServiceTracker<org.osgi.service.startlevel.StartLevel, org.osgi.service.startlevel.StartLevel>			startLevelService;
	@SuppressWarnings("deprecation")
	final ServiceTracker<org.osgi.service.packageadmin.PackageAdmin, org.osgi.service.packageadmin.PackageAdmin>	packageAdmin;

	@SuppressWarnings("deprecation")
	public Core(BundleContext context, DTOFormatter formatter) {
		this.context = context;
		this.startLevelService = new ServiceTracker<>(context, org.osgi.service.startlevel.StartLevel.class, null);
		this.startLevelService.open();
		this.packageAdmin = new ServiceTracker<>(context, org.osgi.service.packageadmin.PackageAdmin.class, null);
		this.packageAdmin.open();
		dtos(formatter);
	}

	@Descriptor("Show the Bundle Symbolic Name")
	public String bsn(Bundle b) {
		return b.getSymbolicName();
	}

	/**
	 * Services
	 */
	@Descriptor("shows the services")
	public List<ServiceReference<?>> srv(
	//@formatter:off
		@Descriptor("Registering bundle")
		@Parameter(absentValue = "0", names = { "-b", "--bundle"})
		Bundle owner
	//@formatter:on
	) throws InvalidSyntaxException {
		return srv(owner, Glob.ALL);
	}

	@Descriptor("shows the services and filter")
	public List<ServiceReference<?>> srv(
	//@formatter:off
		@Descriptor("Registering bundle")
		@Parameter(absentValue = "0", names = { "-b", "--bundle"})
		Bundle owner,

		@Descriptor("Filter")
		Glob glob
	//@formatter:on

	) throws InvalidSyntaxException {

		String filter = "(objectClass=*" + glob + "*)";
		ServiceReference<?>[] refs = context.getAllServiceReferences((String) null, filter);

		long bundleId = owner.getBundleId();
		if (refs == null)
			return Collections.emptyList();

		return Stream.of(refs)
			.filter(ref -> {
				return bundleId == 0L || bundleId == ref.getBundle()
					.getBundleId();
			})
			.collect(Collectors.toList());
	}

	@Descriptor("shows the service")
	public ServiceReference<?> srv(int id) throws InvalidSyntaxException {
		ServiceReference<?>[] allServiceReferences = context.getAllServiceReferences((String) null,
			"(service.id=" + id + ")");
		if (allServiceReferences == null)
			return null;
		assert allServiceReferences.length == 1;
		return allServiceReferences[0];
	}

	/**
	 * Startlevel
	 */
	@Descriptor("query the bundle start level")
	public BundleStartLevel startlevel(@Descriptor("bundle to query")
	Bundle bundle) {

		BundleStartLevel startlevel = bundle.adapt(BundleStartLevel.class);
		if (startlevel == null)
			return null;

		return startlevel;
	}

	@Descriptor("Set the start level of bundles")
	public void startlevel(@Descriptor("startlevel, >0")
	int startlevel, @Descriptor("bundles to set. No bundles imply all bundles except the framework bundle")
	Bundle bundle) {

		if (bundle.getBundleId() == 0L)
			return;

		BundleStartLevel s = bundle.adapt(BundleStartLevel.class);
		s.setStartLevel(startlevel);
	}

	enum Modifier {
		framework,
		initial
	}

	@Descriptor("query the framework start level")
	public FrameworkStartLevel startlevel() {
		return context.getBundle(0L)
			.adapt(FrameworkStartLevel.class);
	}

	//@formatter:off
	@Descriptor("set either the framework or the initial bundle start level")
	public int startlevel(

		@Parameter(names = {"-w", "--wait"}, absentValue = "false", presentValue = "true")
		boolean wait,

		Modifier modifier,

		@Descriptor("either framework or initial level. If <0 then not set, currently value returned")
		int level
	) throws InterruptedException { //@formatter:on

		FrameworkStartLevel fsl = this.startlevel();
		switch (modifier) {
			case framework : {
				int oldlevel = fsl.getStartLevel();
				if (level >= 0) {
					if (wait) {
						Semaphore s = new Semaphore(0);
						fsl.setStartLevel(level, e -> {
							s.release();
						});
						s.acquire();
					} else {
						fsl.setStartLevel(level);
					}
				}
				return oldlevel;
			}

			case initial : {
				int oldlevel = fsl.getInitialBundleStartLevel();
				fsl.setInitialBundleStartLevel(level);
				return oldlevel;
			}
			default :
				throw new IllegalArgumentException("invalid modifier " + modifier);
		}
	}

	enum Sort {
		id,
		bsn,
		level,
		time
	}

	@Descriptor("List all current bundles")
	public List<Bundle> lb(
	//@formatter:off

		@Descriptor("show only the not active bundles")
		@Parameter(absentValue="false", presentValue="true", names= {"-n", "--notactive"})
		boolean notactive,

		@Descriptor("sort by: id | bsn | time | level. Default is an ascending sort")
		@Parameter(absentValue="id", names= {"-s", "--sort"})
		Sort sort,

		@Descriptor("sort in descending order (the default is ascending)")
		@Parameter(absentValue="false", presentValue="true", names= {"-d", "--descending"})
		boolean descending,

		Glob... matches


		//@formatter:on
	) {
		Comparator<Bundle> cmp;
		if ( sort == null)
			sort = Sort.id;

		switch(sort) {
			default :
			case id:
				cmp = (a,b) -> Long.compare( a.getBundleId(), b.getBundleId());
				break;

			case bsn :
				cmp = (a, b) -> a.getSymbolicName()
					.compareTo(b.getSymbolicName());
				break;

			case level :
				cmp = (a, b) -> Integer.compare(startlevel(a).getStartLevel(), startlevel(b).getStartLevel());
				break;

			case time :
				cmp = (a, b) -> Long.compare(a.getLastModified(), b.getLastModified());
				break;
		}
		if (descending) {
			Comparator<Bundle> old = cmp;
			cmp = (a, b) -> old.compare(b, a);
		}


		return Arrays.asList(context.getBundles())
			.stream()
			.filter(k -> !notactive || in(k.getState(), ~Bundle.ACTIVE))
			.sorted(cmp)
			.filter(k -> any(matches, k.getSymbolicName()))
			.collect(Collectors.toList());
	}

	private boolean in(int state, int... s) {
		for (int x : s) {
			if ((x & state) != 0)
				return true;
		}
		return false;
	}

	private boolean any(Glob[] matches, String symbolicName) {
		if (matches == null || matches.length == 0)
			return true;

		for (Glob g : matches) {
			if (g.finds(symbolicName) >= 0)
				return true;
		}
		return false;
	}

	/**
	 * Headers
	 */
	//@formatter:off
	@Descriptor("display bundle headers")
	public Map<Bundle, Map<String, String>> headers(

		@Descriptor("header name, can be globbed")
		@Parameter(absentValue = "*", names = { "-h", "--header"})
		String header,

		@Descriptor("filter on value, can use globbing")
		@Parameter(absentValue = "*", names = { "-v", "--value"})
		String filter,

		@Descriptor("target bundles, if none specified all bundles are used")
		Bundle... bundles

	)		//@formatter:on
	{
		bundles = ((bundles == null) || (bundles.length == 0)) ? context.getBundles() : bundles;

		Glob hp = new Glob(header);
		Glob vp = new Glob(filter);

		Map<Bundle, Map<String, String>> result = new HashMap<>();

		for (Bundle bundle : bundles) {

			Map<String, String> headers = new TreeMap<>();

			Dictionary<String, String> dict = bundle.getHeaders();
			Enumeration<String> keys = dict.keys();
			while (keys.hasMoreElements()) {
				String k = keys.nextElement();
				String v = dict.get(k);
				if (hp.matcher(k)
					.find()
					&& vp.matcher(v)
						.find())
					headers.put(k, v);
			}
			if (headers.size() > 0)
				result.put(bundle, headers);
		}

		return result;
	}

	@Descriptor("refresh bundles")
	//@formatter:off
	public List<Bundle> refresh(

			@Descriptor("Wait for refresh to finish before returning. The maxium time this will wait is 60 seconds. It will return the affected bundles")
			@Parameter(absentValue="false", presentValue="true", names= {"-w","--wait"})
			boolean wait,

			@Descriptor("target bundles (can be empty). If no bundles are specified then all bundles are refreshed")
			Bundle ... bundles

		// @formatter:on
	) {
		List<Bundle> bs = Arrays.asList(bundles);

		FrameworkWiring fw = context.getBundle(0L)
			.adapt(FrameworkWiring.class);
		if (wait) {
			try {
				Bundle older[] = context.getBundles();
				Semaphore s = new Semaphore(0);
				fw.refreshBundles(bs, e -> {
					if (e.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
						s.release();
					}
				});
				s.tryAcquire(60000, TimeUnit.MILLISECONDS);
				Bundle newer[] = context.getBundles();

				Arrays.sort(older, (a, b) -> Long.compare(a.getBundleId(), b.getBundleId()));
				Arrays.sort(newer, (a, b) -> Long.compare(a.getBundleId(), b.getBundleId()));
				return diff(older, newer);
			} catch (InterruptedException e1) {
				// ignore, just return
				return null;
			}
		} else {
			fw.refreshBundles(bs);
			return null;
		}
	}

	private List<Bundle> diff(Bundle[] older, Bundle[] newer) {
		List<Bundle> diffs = new ArrayList<>();
		int o = 0, n = 0;
		while (o < older.length || n < older.length) {

			if (o < older.length && n < older.length) {
				if (older[o].getBundleId() == newer[n].getBundleId()) {
					if (older[o].getLastModified() != newer[n].getLastModified()) {
						diffs.add(older[o]);
					}
					o++;
					n++;
				} else {
					if (older[o].getBundleId() < newer[n].getBundleId()) {
						diffs.add(older[o]);
						o++;
					} else {
						diffs.add(newer[n]);
						n++;
					}
				}
			} else if (o < older.length) {
				diffs.add(older[o]);
				o++;
			} else {
				diffs.add(newer[n]);
				n++;
			}
		}
		return diffs;
	}

	@Descriptor("resolve bundles")
	public List<Bundle> resolve(
	//@formatter:off
		@Descriptor("to be resolved bundles. If no bundles are specified then all bundles are attempted to be resolved")
		Bundle ... bundles

		//@formatter:on
	) {
		List<Bundle> bs = Arrays.asList(bundles);

		FrameworkWiring fw = context.getBundle(0L)
			.adapt(FrameworkWiring.class);
		fw.resolveBundles(bs);
		return lb(false, null, false).stream()
			.filter(b -> (b.getState() & Bundle.UNINSTALLED + Bundle.INSTALLED) != 0)
			.collect(Collectors.toList());
	}

	@Descriptor("start bundles")
	public void start(
	//@formatter:off

		@Descriptor("start bundle transiently")
		@Parameter(names = {"-t", "--transient"}, presentValue = "true", absentValue = "false")
		boolean trans,

		@Descriptor("use declared activation policy")
		@Parameter(names = {"-p", "--policy"}, presentValue = "true", absentValue = "false")
		boolean policy,

		@Descriptor("target bundle")
		Bundle ...bundles

		//@formatter:on
	) throws BundleException {
		int options = 0;

		// Check for "transient" switch.
		if (trans) {
			options |= Bundle.START_TRANSIENT;
		}

		// Check for "start policy" switch.
		if (policy) {
			options |= Bundle.START_ACTIVATION_POLICY;
		}

		for (Bundle bundle : bundles)
			bundle.start(options);
	}

	@Descriptor("stop bundles")
	public void stop(
	// @formatter:off
		@Parameter(names = {"-t", "--transient"}, presentValue = "true", absentValue = "false")
		@Descriptor( "stop bundle transiently")
		boolean trans,

		@Descriptor("target bundles")
		Bundle ...bundles
	// @formatter:on
	) throws BundleException {
		int options = 0;

		if (trans) {
			options |= Bundle.STOP_TRANSIENT;
		}

		for (Bundle bundle : bundles)
			bundle.stop(options);
	}

	@Descriptor("uninstall bundles")
	public void uninstall(
	//@formatter:off

		@Descriptor("the bundles to uninstall")
		Bundle ... bundles

		// @formatter:on
	) throws BundleException {
		for (Bundle bundle : bundles) {
			bundle.uninstall();
		}
	}

	@Descriptor("update bundle")
	public void update(
	//@formatter:off

		@Descriptor("the bundles to update")
		Bundle ... bundles

		// @formatter:on
	) throws BundleException {
		for (Bundle b : bundles) {
			b.update();
		}
	}

	@Descriptor("update bundle from URL")
	public void update(
	// @formatter:off
		CommandSession session,

		@Descriptor("bundle to update")
		Bundle bundle,

		@Descriptor("URL from where to retrieve bundle")
		String location

	//@formatter:on
	) throws IOException, BundleException {

		Objects.requireNonNull(bundle);
		Objects.requireNonNull(location);

		location = Util.resolveUri(session, location.trim());
		InputStream is = new URL(location).openStream();
		bundle.update(is);
	}

	@Descriptor("determines the class loader for a class name and a bundle")
	public ClassLoader which(
	//@formatter:off

		@Descriptor("the bundle to load the class from")
		Bundle bundle,

		@Descriptor("the name of the class to load from bundle")
		String className

		//@formatter:on
	) throws ClassNotFoundException {
		Objects.requireNonNull(bundle);
		Objects.requireNonNull(className);

		Class<?> clazz = null;
		return bundle.loadClass(className)
			.getClassLoader();
	}

	void dtos(DTOFormatter f) {
		f.build(Bundle.class)
			.inspect()
			.method("bundleId")
			.format("STATE", Core::state)
			.method("symbolicName")
			.method("version")
			.method("location")
			.format("LAST MODIFIED", b -> DisplayUtil.lastModified(b.getLastModified()))
			.format("servicesInUse", b -> b.getServicesInUse())
			.method("registeredServices")
			.format("HEADERS", Bundle::getHeaders)
			.part()
			.as(b -> "[" + b.getBundleId() + "] " + b.getSymbolicName())
			.line()
			.method("bundleId")
			.format("STATE", Core::state)
			.method("symbolicName")
			.method("version")
			.format("START LEVEL", this::startlevel)
			.format("LAST MODIFIED", b -> DisplayUtil.lastModified(b.getLastModified()));

		f.build(BundleDTO.class)
			.inspect()
			.fields("*")
			.line()
			.field("id")
			.field("symbolicName")
			.field("version")
			.field("state")
			.part()
			.as(b -> String.format("[%s]%s", b.id, b.symbolicName));

		f.build(ServiceReference.class)
			.inspect()
			.format("id", s -> getServiceId(s) + "")
			.format("objectClass", this::objectClass)
			.format("bundle", s -> s.getBundle()
				.getBundleId() + "")
			.format("usingBundles", s -> bundles(s.getUsingBundles()))
			.format("properties", DisplayUtil::toMap)
			.line()
			.format("id", s -> getServiceId(s) + "")
			.format("bundle", s -> s.getBundle()
				.getBundleId() + "")
			.format("service", this::objectClass)
			.format("ranking", s -> s.getProperty(Constants.SERVICE_RANKING))
			.format("component", s -> s.getProperty("component.id"))
			.format("usingBundles", s -> bundles(s.getUsingBundles()))
			.part()
			.as(s -> String.format("(%s) %s", getServiceId(s), objectClass(s)));

		f.build(BundleStartLevel.class)
			.inspect()
			.format("level", s -> s.getStartLevel() + "")
			.format("persistent", s -> s.isPersistentlyStarted())
			.format("act. policy", s -> s.isActivationPolicyUsed())
			.line()
			.format("level", s -> s.getStartLevel() + "")
			.format("persistent", s -> s.isPersistentlyStarted())
			.format("act. policy", s -> s.isActivationPolicyUsed())
			.part()
			.as(s -> String.format("%s %s", s.getStartLevel(),
				(s.isPersistentlyStarted() ? "" : "T") + (s.isActivationPolicyUsed() ? "A" : "")));
	}

	private String bundles(Bundle[] usingBundles) {
		if (usingBundles == null)
			return null;

		return Stream.of(usingBundles)
			.map(b -> b.getBundleId() + "")
			.collect(Collectors.joining("\n"));
	}

	private long getServiceId(ServiceReference<?> s) {
		return (Long) s.getProperty(Constants.SERVICE_ID);
	}

	String objectClass(ServiceReference<?> ref) {
		return DisplayUtil.objectClass(DisplayUtil.toMap(ref));
	}

	private static String state(Bundle b) {

		switch (b.getState()) {
			case Bundle.ACTIVE :
				return "ACTV";
			case Bundle.INSTALLED :
				return "INST";
			case Bundle.RESOLVED :
				return "RSLV";
			case Bundle.STARTING :
				return "⬆︎︎";
			case Bundle.STOPPING :
				return "⬇︎︎";
			case Bundle.UNINSTALLED :
				return "UNIN";
		}
		return null;
	}
}
