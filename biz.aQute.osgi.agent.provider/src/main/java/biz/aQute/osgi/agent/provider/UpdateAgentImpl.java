package biz.aQute.osgi.agent.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import biz.aQute.osgi.agent.api.UpdateAgent;
import biz.aQute.osgi.agent.dto.BundleRefDTO;
import biz.aQute.osgi.agent.dto.ConfigDTO;

/**
 * Implements a background thread that polls a remote server for a
 * configuration. It will then attempt to synchronize the internal bundles with
 * the configuration's bundles by installing, updating, or uninstalling bundles.
 * <p>
 * This code goes out of its way to get this done using recovery methods.
 */
@SuppressWarnings("deprecation")
class UpdateAgentImpl implements UpdateAgent, Runnable {

	private static final long					POLL_TIME	= 30000;
	private final BundleContext					context;
	private final Downloader					downloader;
	private final DigestVerifier				verifier;
	private final PackageAdmin					packageAdmin;
	private final AtomicBoolean					enabled		= new AtomicBoolean();
	private final AtomicBoolean					open		= new AtomicBoolean();
	private final AtomicBoolean					triggered	= new AtomicBoolean();
	private final TransactionStore<ConfigDTO>	store;
	private final Executor						executor;
	private int									retries		= 3;
	private volatile State						state;
	private Thread								thread;
	private URI									configUrl;

	enum Phase {
		VETOED, COMMITTED, OK
	}

	UpdateAgentImpl(BundleContext context, Executor executor, PackageAdmin packageAdmin, Downloader downloader,
			DigestVerifier verifier)
			throws Exception {
		this.executor = executor;
		this.packageAdmin = packageAdmin;
		this.downloader = downloader;
		this.context = context;
		this.verifier = verifier;
		this.thread = new Thread(this, "UpdateAgent");

		File dir = getAgentDir(context);
		this.store = new TransactionStore<>(dir, 3, ConfigDTO.class, getClass().getResource("fallbackresource.json"));

		this.thread.start();
	}

	@Override
	public boolean disable() {
		boolean v = enabled.getAndSet(false);
		thread.interrupt();
		return v;
	}

	@Override
	public boolean enable() {
		boolean v = enabled.getAndSet(true);
		thread.interrupt();
		return v;
	}

	@Override
	public boolean trigger() {

		if (enabled.get())
			throw new IllegalStateException("Not enabled");

		return triggered.getAndSet(true);
	}

	@Override
	public UpdateAgent.State getState() {
		return state;
	}

	public void run() {
		open.set(true);

		while (open.get())
			try {

				state = State.WAITING;

				if (!triggered.getAndSet(false))
					Thread.sleep(POLL_TIME);

				if (enabled.get()) {
					update();
				}

			} catch (InterruptedException ie) {
				// ignore
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	void update() throws Exception {
		state = State.DOWNLOAD_CONFIG;

		ConfigDTO currentConfig = store.read();

		ConfigDTO nextConfig = downloader.download(configUrl, ConfigDTO.class).getValue();

		Phase phase = update(nextConfig.bundles);
		if (phase == Phase.OK) {

			//
			// All ended ok, we can make this configuration
			// the current configuration
			//
			store.update(nextConfig);

		} else if (phase == Phase.COMMITTED) {
			//
			// Recover from to previous configuration
			//
			update(currentConfig.bundles);
		}
	}

	Phase update(List<BundleRefDTO> bundleDtos) throws InterruptedException {
		Phase phase = Phase.OK;
		for (int retry = 0; retry < retries; retry++) {
			try {

				List<BundleAction> actions = compare(bundleDtos);

				if (actions.isEmpty()) {
					System.out.println("Synced");
					return Phase.OK;
				}

				//
				// Ensure we first do uninstalls and any other ordering
				//

				Collections.sort(actions);

				state = State.PREPARING;

				List<Promise<Void>> list = forEach(actions, BundleAction::prepare);
				sync(list); // ensure all files are downloaded

				try {
					state = State.STOPPING;

					list = forEach(actions, BundleAction::stop);
					sync(list); // ensure all bundles are stopped

					state = State.COMMITTING;

					for (BundleAction a : actions) {
						a.commit();
						phase = Phase.COMMITTED;
					}

					refresh();
					
					state = State.STARTING;
					list = forEach(actions, BundleAction::start);
					sync(list); // ensure all bundles are started

					return Phase.OK;

				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
				state = State.RETRY_WAIT;
				Thread.sleep(10000 * (retry + 1));
			}
		}
		return phase;
	}

	private void refresh() throws InterruptedException {
		Semaphore s = new Semaphore(0);
		FrameworkListener listener = e -> {
			if (e.getType() == FrameworkEvent.PACKAGES_REFRESHED)
				s.release();
		};
		try {
			context.addFrameworkListener(listener);
			packageAdmin.refreshPackages(null);
			s.tryAcquire(300, TimeUnit.SECONDS);
		} finally {
			context.removeFrameworkListener(listener);
		}
	}

	private List<Void> sync(List<Promise<Void>> list) throws InvocationTargetException, InterruptedException {
		return Promises.all(list).getValue();
	}

	private List<Promise<Void>> forEach(List<BundleAction> actions,
			Function<? super BundleAction, ? extends Promise<Void>> mapper) {
		return actions
				.stream()
				.map(mapper)
				.collect(Collectors.toList());
	}

	private List<BundleAction> compare(List<BundleRefDTO> bundles) throws IOException {
		List<BundleAction> actions = new ArrayList<>();
		Map<String, BundleRefDTO> index = bundles
				.stream()
				.collect(Collectors.toMap(brf -> brf.location, x -> x));

		for (Bundle bundle : context.getBundles()) {

			if (isProtected(bundle))
				continue;

			String location = bundle.getLocation();
			BundleRefDTO ref = index.remove(location);

			if (ref == null) {
				actions.add(new UninstallAction(bundle));
				continue;
			}

			if (!verifier.verifyDigest(bundle, ref.digest))
				actions.add(new UpdateAction(bundle, ref));
			else
				actions.add(new StartAction(bundle));
		}

		for (BundleRefDTO ref : index.values()) {
			actions.add(new InstallAction(ref));
		}

		return actions;
	}

	private boolean isProtected(Bundle bundle) {
		String location = bundle.getLocation();
		return !location.startsWith("SLM:") && !location.startsWith("generated ");
	}

	public void close() throws InterruptedException {
		if (open.getAndSet(false)) {
			thread.interrupt();
			thread.join(10000);
		}
	}

	Promise<InputStream> download(BundleRefDTO ref) {
		return downloader.download(ref.path, InputStream.class);
	}

	interface RunnableWithException {
		void run() throws Exception;
	}

	private Promise<Void> background(RunnableWithException run) {
		Deferred<Void> deferred = new Deferred<>();

		executor.execute(() -> {

			try {

				run.run();
				deferred.resolve(null);

			} catch (Exception e) {
				deferred.fail(e);
			}
		});

		return deferred.getPromise();
	}

	abstract class BundleAction implements Comparable<BundleAction> {
		Promise<Void> prepare() {
			return Promises.resolved(null);
		}

		Promise<Void> stop() {
			return Promises.resolved(null);
		}

		void commit() throws Exception {
		}

		Promise<Void> start() {
			return Promises.resolved(null);
		}

		@Override
		public int compareTo(BundleAction o) {
			if (o.getClass() == this.getClass())
				return 0;

			if (this.getClass() == UpdateAction.class)
				return 1;

			return -1;
		}

	}

	class InstallAction extends BundleAction {
		private final BundleRefDTO		ref;
		private Promise<InputStream>	result;
		public Bundle					bundle;

		InstallAction(BundleRefDTO ref) {
			this.ref = ref;
		}

		@Override
		Promise<Void> prepare() {
			result = download(ref);
			return result.map(v -> (Void) null);
		}

		@Override
		void commit() throws Exception {
			InputStream inputStream = result.getValue();

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			inputStream = new DigestInputStream(inputStream, md);

			bundle = doAction(inputStream);

			if (!Arrays.equals(md.digest(), ref.digest)) {
				bundle.uninstall();
				throw new IllegalArgumentException("Invalid digest " + ref + " is " + Arrays.toString(ref.digest));
			}
			verifier.updateDigest(bundle, ref.digest);
		}

		protected Bundle doAction(InputStream inputStream) throws BundleException {
			return context.installBundle(ref.location, inputStream);
		}

		@Override
		Promise<Void> start() {
			return background(bundle::start);
		}

	}

	class UpdateAction extends InstallAction {
		UpdateAction(Bundle bundle, BundleRefDTO ref) {
			super(ref);
			this.bundle = bundle;
		}

		Promise<Void> stop() {
			return background(this.bundle::stop);
		}

		@Override
		protected Bundle doAction(InputStream inputStream) throws BundleException {
			bundle.update(inputStream);
			return bundle;
		}
	}

	class UninstallAction extends BundleAction {
		private final Bundle bundle;

		UninstallAction(Bundle bundle) {
			this.bundle = bundle;
		}

		Promise<Void> stop() {
			return background(() -> bundle.stop());
		}

		public void commit() throws BundleException {
			assert (bundle.getState() & (Bundle.RESOLVED + Bundle.INSTALLED)) != 0;
			bundle.uninstall();
		}

	}

	class StartAction extends BundleAction {
		private final Bundle bundle;

		StartAction(Bundle bundle) {
			this.bundle = bundle;
		}

		@Override
		Promise<Void> start() {
			return background(bundle::start);
		}

	}

	public void setConfigURL(URI uri) {
		configUrl = uri;
	}

	private File getAgentDir(BundleContext context) {
		File dir = context.getDataFile(".agent");
		if (!dir.isDirectory()) {
			if (!dir.mkdirs())
				throw new IllegalStateException("Cannot create .agent directory to store current config");
		}
		return dir;
	}

}
