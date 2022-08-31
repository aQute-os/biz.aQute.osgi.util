package biz.aQute.aggregate.provider;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concurrency is hard. This class abstracts the pattern where you have a
 * potentially long running initialization and a possibility that the component
 * must be closed before the initialization is started or finished. Since the
 * initialization process must not run in a lock there are a surprising number
 * of states in this 'simple' problem. Additionally, this class can leave the
 * finishing of an initialization and its closing be done sometime later or it
 * can wait until the close is finished: syncClose.
 * <p>
 * The init & close methods are always run outside the lock held by this object.
 * The init must return an AutoCloseable that is used to deinitialize.
 * <p>
 * This object is a {@link Runnable} and can therefore be passed to an Executor
 * to run in the background. Once in the background, the initialization will be
 * started (if not yet closed). Normally after the init lambda returns, the
 * state will become active until this object is closed.
 * <p>
 *
 * <pre>
 *   class X implements AutoCloseable {
 *      static final ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();
 *
 *      final InitClose		ic = new InitClose( this::init, false );
 *
 *      X() {
 *         schedule(ic);
 *      }
 *
 *      public void close() {
 *         ic.close();
 *         es.shutDown();
 *      }
 *
 *      AutoCloseable init() {
 *            .... heavy initialization
 *            return () -> .... clean up all that work
 *      }
 *
 *  }
 * </pre>
 */
public class InitClose implements AutoCloseable, Runnable {

	final Supplier<AutoCloseable>	init;
	final boolean					syncClose;

	AutoCloseable					result;
	Thread							thread;
	String							message;
	State							state	= State.INITIAL;

	enum State {
		INITIAL,
		BUSY,
		SYNCING_CLOSE,
		ACTIVE,
		CLOSED,
		ERRORED;
	}

	/**
	 * Create an InitClose
	 *
	 * @param init the initialization function
	 * @param syncClose true indicates that close will with for initialization &
	 *            its close to finish before it has started. Otherwise this is
	 *            done in the background
	 */
	public InitClose(Supplier<AutoCloseable> init, boolean syncClose) {
		Objects.requireNonNull(init, "init");
		this.init = init;
		this.syncClose = syncClose;
	}

	/**
	 * The Runnable. To make this work, submit this Runnable to a an executor.
	 */
	@Override
	public void run() {
		start();
		AutoCloseable c;
		try {
			c = init.get();
		} catch (Exception e) {
			throw error("exception in the initialization " + e);
		}
		if (c == null)
			throw error("expected a non null value");
		set(c);
	}

	/*
	 * Start event.
	 */
	synchronized void start() {
		switch (state) {
			case INITIAL :
				this.state = State.BUSY;
				this.thread = Thread.currentThread();
				return;

			case ERRORED :
			case CLOSED :
				// ignore, we got closed before we got in the background
				// thread
				return;

			default :
				throw error("unknown state");
		}
	}

	void set(AutoCloseable c) {
		synchronized (this) {
			switch (state) {
				case BUSY :
					this.result = c;
					this.thread = null;
					this.state = State.ACTIVE;
					return;

				case SYNCING_CLOSE :
					this.result = null;
					this.thread = null;
					break; // <-------------- close it

				case ERRORED :
				case CLOSED :
					break; // <-------------- close it

				default :
					throw error("unexpected state");
			}
		}
		close(c);
		synchronized (this) {
			switch (state) {
				case SYNCING_CLOSE :
					this.state = State.CLOSED;
					notifyAll();
					return;

				case ERRORED :
				case CLOSED :
					return;

				default :
					throw error("unexpected state");
			}
		}

	}

	@Override
	public void close() throws Exception {
		AutoCloseable c;
		synchronized (this) {
			switch (state) {

				case ACTIVE :
					c = result;
					result = null;
					if (syncClose) {
						state = State.SYNCING_CLOSE;
					} else
						state = State.CLOSED;
					break; // <----------------

				case BUSY :
					if (syncClose) {
						state = State.SYNCING_CLOSE;
						thread.interrupt();
						while (state == State.SYNCING_CLOSE)
							wait();
					} else {
						thread.interrupt();
						state = State.CLOSED;
					}
					return;

				case ERRORED :
				case CLOSED :
					return;

				default :
					throw error("unexpected state");
			}
		}
		close(c);
		synchronized (this) {
			switch (state) {
				case SYNCING_CLOSE :
					state = State.CLOSED;
					return;

				case ERRORED :
				case CLOSED :
					return;

				default :
					throw error("unknown state");
			}
		}
	}

	static void close(AutoCloseable c) {
		try {
			if (c != null)
				c.close();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private IllegalStateException error(String string) {
		this.message = string;
		this.state = State.ERRORED;
		return new IllegalStateException(string);
	}

}
