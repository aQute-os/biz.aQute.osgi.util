package biz.aQute.trace.activate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class ActivationTracer {

	final static long							epoch	= System.nanoTime();
	final static long							epochM	= System.currentTimeMillis();
	final static AtomicInteger					idg		= new AtomicInteger(1000);

	public static class Event {
		public int			id		= idg.incrementAndGet();
		public String		type;
		public long			begin;
		public long			end;
		public long			bundle;
		public String		methodName;
		public String		thread;
		public List<Integer>	next	= new ArrayList<>();
		public int			prevId;
		Event				prev;

		@Override
		public String toString() {
			return String.format("%04d %10s %10s [%d] %s", id, getDuration(begin), getDuration(end - begin), bundle,
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

	public static void event(Object component, String methodName, String type, String inOut) {

		synchronized (events) {
			if (inOut.equals(">")) {
				Event event = new Event();
				event.type = type;
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
					lastEvent.set(event);
				}

				index.put(methodName, event);
				events.add(event);
			} else if (inOut.equals("<")) {
				Event event = index.remove(methodName);
				if (event != null) {
					assert type.equals(event.type);
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

	public static void clear() {
		synchronized (events) {
			events.clear();
		}
	}

	public static List<Event> list() {
		return events;
	}
}
