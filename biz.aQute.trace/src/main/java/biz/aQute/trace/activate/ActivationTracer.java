package biz.aQute.trace.activate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Public class to manage the event queue for tracing and the extra classes that
 * needs to be woven
 */
public class ActivationTracer {

	public static boolean						debug;
	public final static Map<String, String[]>	extra	= new HashMap<>();

	static long									epoch;
	static long									epochM;
	final static AtomicInteger					idg		= new AtomicInteger(1000);

	/**
	 * Holds an even
	 */
	public static class Event {
		public int				id		= idg.incrementAndGet();
		public String			action;
		public long				begin;
		public long				end;
		public long				bundle;
		public String			methodName;
		public String			thread;
		public List<Integer>	next	= new ArrayList<>();
		public int				prevId;
		Event					prev;

		@Override
		public String toString() {
			return String.format("%04d %10s %10s %-6s [%d] %s", id, getDuration(begin), getDuration(end - begin),
				action, bundle,
				methodName);
		}

		private String getDuration(long duration) {
			if (duration < 1000)
				return duration + " ns ";

			duration /= 1000;
			if (duration < 1000)
				return duration + " µs ";

			duration /= 1000;
			if (duration < 1000)
				return duration + " ms ";
			duration /= 1000;
			if (duration < 1000)
				return duration + " sec";
			duration /= 60;
			if (duration < 60)
				return duration + " min";

			duration /= 60;
			return duration + " h  ";
		}

	}

	static final List<Event>		events		= new ArrayList<>();
	static final Map<String, Event>	index		= new HashMap<>();
	static final ThreadLocal<Event>	lastEvent	= new ThreadLocal<>();

	static {
		clear();
	}

	/**
	 * Create an event in the queue
	 *
	 * @param component the object that generates the even
	 * @param methodName the name of the method
	 * @param action the type of event (A=Activate, D=Deactive, M=Modified,
	 *            etc.)
	 * @param inOut is '>' for entering and '<' for exiting
	 */
	public static void event(Object component, String methodName, String action, String inOut) {
		if (debug)
			System.out.println("event " + component + " " + methodName + " " + action + " " + inOut);

		synchronized (events) {
			if (inOut.equals(">")) {
				Event event = new Event();
				event.action = action;
				event.thread = Thread.currentThread()
					.getName();
				event.begin = System.nanoTime() - epoch;
				event.methodName = methodName;
				Bundle bundle = FrameworkUtil.getBundle(component.getClass());
				if (bundle != null) {
					event.bundle = bundle.getBundleId();
				}

				Event prev = lastEvent.get();
				if (prev != null) {
					prev.next.add(event.id);
					event.prev = prev;
					event.prevId = prev.id;
				}
				lastEvent.set(event);

				index.put(methodName, event);
				events.add(event);
			} else if (inOut.equals("<")) {
				Event event = index.remove(methodName);
				if (event != null) {
					assert action.equals(event.action);
					event.end = System.nanoTime() - epoch;

					lastEvent.set(event.prev);
				} else {
					System.err.println("Orphan out event " + methodName);
				}
			} else {
				System.err.println("Unknown inOut" + inOut);
			}
		}
	}

	/**
	 * Clear the event queue
	 */
	public static void clear() {
		synchronized (events) {
			events.clear();
			epoch = System.nanoTime();
			epochM = System.currentTimeMillis();
			idg.set(1000);
		}
	}

	/**
	 * List the raw even queue
	 */
	public static List<Event> list() {
		return events;
	}

	/**
	 * Trace a method.
	 *
	 * @param spec Format <class-fqn>:method:action
	 */
	public static void trace(String spec) {
		String args[] = spec.split(":");
		if (args.length != 3) {
			System.err.println("Invalid definition " + spec + " not three parts separated by ':'");
		} else {
			assert args.length == 3 : "Extra must be <class>:<method>:<action>";
			extra.put(args[0], args);
		}
	}

	/**
	 * Untrace an earlier traced method
	 *
	 * @param spec Format <class-fqn>(:method:action )?
	 */
	public static void untrace(String spec) {
		String args[] = spec.split(":");
		String clazz;
		if (args.length == 1) {
			clazz = args[0];
		} else if (args.length == 3) {
			clazz = args[0];
		} else {
			System.err.println("Invalid format for unweave " + spec + " not three parts separated by ':' or classname");
			return;
		}

		if (extra.remove(clazz) == null)
			System.err.println("No " + spec + " not three parts separated by ':'");
	}

}
