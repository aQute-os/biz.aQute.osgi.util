package biz.aQute.aggregate.provider;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * Detecting if the framework has finished starting all the bundles is
 * difficult. If a bundle is started after launch, there will be a
 * {@link FrameworkEvent#STARTED} event. However, updates and other bundle
 * activities can cause a bundle to be started after the framework has finished
 * starting. There is as far as I know no way to find out if the framework is in
 * launching mode or in normal maintenance mode. We therefore wait for a
 * {@link FrameworkEvent#STARTED} event or timeout after there is period of no
 * activity in bundle installation.
 * <p>
 * A tip, register a FRAMEWORK_BEGINNING_STARTLEVEL property of 2 if no start
 * levels are used. This will allow the bundle to detect that the framework has
 * finished starting all the bundles. Otherwise, this class tends to introduce a
 * delay of the used timeout.
 */
public class FrameworkStartedDetector {
	static final String		INACTIVITY_TIMEOUT			= "biz.aQute.startup.timeout.in.ms";
	static final String		INACTIVITY_TIMEOUT_DEFAULT	= "3_000";
	final BundleContext		context;
	final long				timeout;
	final CountDownLatch	started						= new CountDownLatch(1);

	volatile long			lastModified				= System.nanoTime();
	volatile Reason			reason						= Reason.WAITING;

	/**
	 * The reason how the start was detected.
	 */
	public enum Reason {
		WAITING,
		STARTED_EVENT,
		STARTLEVEL,
		INTERRUPTED,
		TIMEOUT
	}

	public FrameworkStartedDetector(BundleContext context) {
		this.context = context;
		this.timeout = Long.getLong(INACTIVITY_TIMEOUT, 3_000);
	}

	/**
	 * Wait for the framework to start. This is defined as receiving a
	 * {@link FrameworkEvent#STARTED} event or no bundle event for more than
	 * timeout seconds. Where timeout can be send with the
	 * {@value FrameworkStartedDetector#INACTIVITY_TIMEOUT} system property in
	 * milliseconds. The default is {@value #INACTIVITY_TIMEOUT_DEFAULT}.
	 *
	 * @return the reason for deciding the framework is started
	 */
	public Reason waitForStart() {
		return waitForStart(timeout);
	}

	/**
	 * Wait for the framework to start. This is defined as receiving a
	 * {@link FrameworkEvent#STARTED} event or no bundle event for more than
	 * timeout seconds.
	 *
	 * @return true if received {@link FrameworkEvent#STARTED}, when timed out
	 *         or interrupted false.
	 */
	public Reason waitForStart(long timeout) {
		if (reason != Reason.WAITING)
			return reason;

		String beginning = context.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
		int beginningStartevel = toInt(beginning, 2);
		if (beginningStartevel == 1) {
			// with a beginning startlevel of 1 it is clear we do not have a
			// higher beginning startlevel than the highest bundle sl. So we
			// cannot use this mechanism. It is tricky with higher startlevels
			// because it is possible to set the begginning start level to the
			// highest level bundle but it is custom to do +1 for the highest
			// startlevel used.
			beginningStartevel = 2; // disables startlevel check
		}

		FrameworkStartLevel fwsl = context.getBundle(0L)
			.adapt(FrameworkStartLevel.class);

		BundleListener bundleListener = this::bundleChanged;
		FrameworkListener frameworkListener = this::frameworkEvent;
		try {
			this.context.addBundleListener(bundleListener);
			this.context.addFrameworkListener(frameworkListener);

			while (true)
				try {
					if (fwsl.getStartLevel() == beginningStartevel)
						return reason = Reason.STARTLEVEL;

					if (started.await(100, TimeUnit.MILLISECONDS)) {
						System.out.println("latch worked");
						return reason = Reason.STARTED_EVENT;
					}
					long diff = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastModified);
					if (diff > timeout) {
						System.out.println("timed out");
						return reason = Reason.TIMEOUT;
					}
				} catch (InterruptedException ie) {
					Thread.currentThread()
						.interrupt();
					return reason = Reason.INTERRUPTED;
				}
		} finally {
			this.context.removeBundleListener(bundleListener);
			this.context.removeFrameworkListener(frameworkListener);
		}
	}

	/**
	 * The current reason. Will always be not {@link Reason#WAITING} after the
	 * {@link #waitForStart()} has returned.
	 *
	 * @return the current reason
	 */
	public Reason getReason() {
		return reason;
	}

	private int toInt(String beginning, int deflt) {
		if (beginning == null)
			return deflt;
		try {
			return Integer.parseInt(beginning);
		} catch (Exception e) {
			return deflt;
		}
	}

	void bundleChanged(BundleEvent event) {
		lastModified = System.nanoTime();
		System.out.println("got a new time");
	}

	void frameworkEvent(FrameworkEvent event) {
		switch (event.getType()) {
			case FrameworkEvent.STARTED :
				System.out.println("got STARTED from fw");
				started.countDown();
				break;
		}
	}
}
