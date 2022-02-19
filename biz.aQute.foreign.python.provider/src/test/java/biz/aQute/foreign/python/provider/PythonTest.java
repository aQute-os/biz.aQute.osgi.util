package biz.aQute.foreign.python.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.felix.service.command.CommandProcessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import biz.aQute.foreign.python.configuration.Configuration;
import biz.aQute.osgi.configuration.util.ConfigSetter;

public class PythonTest {
	static LaunchpadBuilder builder = new LaunchpadBuilder().runfw("org.apache.felix.framework")
		.bundles(
			"org.apache.felix.gogo.runtime,org.apache.felix.gogo.command,org.apache.felix.scr, org.osgi.util.promise, org.osgi.util.function");

	@After
	@Before
	public void delay() throws InterruptedException {
		Thread.sleep(1000);
	}

	@Test
	public void testSimple() throws Exception {
		try (Launchpad lp = builder.debug()
			.create()) {
			lp.bundle()
				.includeResource("python/app.py", "resources/hello.py", false, false)
				.start();
			Thread.sleep(1000);
		}
	}

	@Service
	CommandProcessor			gogo;

	ConfigSetter<Configuration>	cf	= new ConfigSetter<>(Configuration.class);

	/*
	 * See if a forever loop is properly terminated. Also test that a directory
	 * copy works fine.
	 */
	@Test
	public void testForever() throws Exception {

		try (Launchpad lp = builder.debug()
			.create()
			.inject(this)) {

			PythonAdmin admin = new PythonAdmin(lp.getBundleContext(), cf.delegate(), gogo);
			lp.register(PythonAdmin.class, admin);
			assertThat(admin.python()).isEmpty();

			Bundle start = lp.bundle()
				.includeResource("python/app.py", "resources/forever.py", false, false)
				.includeResource("python/foo", "resources", false, false) // to
																			// test
																			// copying
																			// a
																			// directory
				.start();
			assertThat(admin.python()).isNotEmpty();

			Thread.sleep(1000);
			assertThat(admin.python()).isNotEmpty();
			PythonApp pythonApp = admin.python()
				.get(0);
			assertThat(pythonApp.restarts).isEqualTo(0);
			start.stop();
			assertThat(admin.python()).isEmpty();

		}
	}

	/*
	 * Test if we can do a gogo command
	 */

	Semaphore commandCalled = new Semaphore(0);

	public class GogoCommand {
		public int test() {
			System.out.println("called 42");
			commandCalled.release();
			return 42;
		}
	}

	@Test
	public void testGogo() throws Exception {
		try (Launchpad lp = builder.debug()
			.create()
			.inject(this)) {

			PythonAdmin admin = new PythonAdmin(lp.getBundleContext(), cf.delegate(), gogo);
			lp.register(PythonAdmin.class, admin);

			lp.register(Object.class, new GogoCommand(), CommandProcessor.COMMAND_SCOPE, "scope",
				CommandProcessor.COMMAND_FUNCTION, new String[] {
					"test"
				});
			lp.bundle()
				.includeResource("python/app.py", "resources/gogo.py", false, false)
				.start();

			assertThat(commandCalled.tryAcquire(20, TimeUnit.SECONDS)).isTrue();
		}
	}

	@Test
	public void testUpdate() throws Exception {
		try (Launchpad lp = builder.debug()
			.create()
			.inject(this)) {

			PythonAdmin admin = new PythonAdmin(lp.getBundleContext(), cf.delegate(), gogo);
			lp.register(PythonAdmin.class, admin);

			Bundle b = lp.bundle()
				.includeResource("python/app.py", "resources/hello.py", false, false)
				.install();
			b.start();
			PythonApp pythonApp = admin.python()
				.get(0);
			assertThat(pythonApp.copied).isTrue();
			Thread.sleep(5000);
			b.stop();
			Thread.sleep(5000);
			assertThat(admin.python()).isEmpty();
			b.start();
			pythonApp = admin.python()
				.get(0);
			assertThat(pythonApp.copied).isFalse();
		}
	}

	@Test
	public void testRestart() throws Exception {
		ConfigSetter<Configuration> cf = new ConfigSetter<>(Configuration.class);
		cf.set(cf.delegate()
			.restartDelay())
			.to(5000L);

		try (Launchpad lp = builder.debug()
			.create()
			.inject(this)) {

			PythonAdmin admin = new PythonAdmin(lp.getBundleContext(), cf.delegate(), gogo);
			admin.restartDelay = 400;
			lp.register(PythonAdmin.class, admin);

			Bundle b = lp.bundle()
				.includeResource("python/app.py", "resources/error.py", false, false)
				.install();
			b.start();
			Thread.sleep(3000);
			PythonApp pythonApp = admin.python()
				.get(0);
			assertThat(pythonApp.restarts).isGreaterThan(1);
			System.out.println(pythonApp.result);
			assertThat(pythonApp.result).isNotEqualTo(0);
		}
	}
}
