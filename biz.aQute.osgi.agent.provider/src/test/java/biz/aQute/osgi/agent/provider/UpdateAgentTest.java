package biz.aQute.osgi.agent.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;

import aQute.launchpad.BundleBuilder;
import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;

/*
 * Example JUNit test case
 *
 */

@SuppressWarnings("deprecation")
public class UpdateAgentTest {
	private final Executor	executor	= Executors.newWorkStealingPool(4);

	Launchpad			fw			= new LaunchpadBuilder().runfw("org.apache.felix.framework").create().inject(this);

	@Service
	PackageAdmin padmin;
	
	@Test
	public void zeroBundles() throws Exception {
		UpdateAgentImpl agent = init();
		agent.setConfigURL(new URI("file:src/test/resources/configs/zero.json"));
		int n = fw.getBundleContext().getBundles().length;
		agent.update();
		assertEquals(n, fw.getBundleContext().getBundles().length);
	}

	@Test
	public void installOneBundle() throws Exception {
		UpdateAgentImpl agent = init();
		agent.setConfigURL(new URI("file:src/test/resources/configs/install.json"));

		agent.update();
		assertTrue(getBundleByLocation("SLM:one").isPresent());
		assertEquals("biz.aQute.osgi.test.bundles.a", getBSNFromLocation("SLM:one"));
	}

	@Test
	public void installAndThenupdateOneBundle() throws Exception {
		UpdateAgentImpl agent = init();

		agent.setConfigURL(new URI("file:src/test/resources/configs/install.json"));
		agent.update();
		assertTrue(getBundleByLocation("SLM:one").isPresent());
		assertEquals("biz.aQute.osgi.test.bundles.a", getBSNFromLocation("SLM:one"));

		agent.setConfigURL(new URI("file:src/test/resources/configs/update.json"));
		agent.update();
		assertTrue(getBundleByLocation("SLM:one").isPresent());
		assertEquals("biz.aQute.osgi.test.bundles.b", getBSNFromLocation("SLM:one"));
	}

	static public class Act implements BundleActivator {

		@Override
		public void start(BundleContext context) throws Exception {
			System.out.println("Start");
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			System.out.println("Stop");
		}

	}

	@Test
	public void uninstallExistingBundle() throws Exception {
		UpdateAgentImpl agent = init();
		agent.setConfigURL(new URI("file:src/test/resources/configs/zero.json"));

		BundleBuilder bb = fw.bundle();
		bb.bundleActivator(Act.class.getName());
		Bundle installed = bb.install();
		installed.start();
		agent.update();
		//assertEquals(Bundle.UNINSTALLED, installed.getState());
	}


	@Test
	public void recoverFromNewBundleThatThrowsException() throws Exception {
		UpdateAgentImpl agent = init();
		agent.setConfigURL(new URI("file:src/test/resources/configs/activatorexception.json"));
		int n = fw.getBundleContext().getBundles().length;
		agent.update();
		assertEquals(n,fw.getBundleContext().getBundles().length);
		
		assertFalse( getBundleByLocation("SLM:ok").isPresent());
		assertFalse( getBundleByLocation("SLM:exception").isPresent());
		
	}

	void addResource(Class<?> class1, BundleBuilder b) throws IOException {
		String name = class1.getName();
		name = name.replace('.', '/') + ".class";
		b.addResource(name, class1.getResource("/" + name));
	}

	private Optional<Bundle> getBundleByLocation(String location) {
		return Stream.of(fw.getBundleContext().getBundles()).filter(bundle -> bundle.getLocation().equals(location)).findAny();
	}

	private UpdateAgentImpl init() throws Exception {
		UpdateAgentImpl agent = new UpdateAgentImpl(fw.getBundleContext(), executor, padmin, new Downloader(executor),
				new DigestVerifier());
		return agent;
	}

	private String getBSNFromLocation(String location) {
		return getBundleByLocation(location).map(b -> b.getSymbolicName()).orElse(null);
	}

}
