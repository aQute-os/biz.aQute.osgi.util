package biz.aQute.aggregate.provider;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings({ "rawtypes", "unchecked" })
class AggregateState implements AutoCloseable {

	final BundleContext					context;
	final Map<Class, TrackedService>	trackedServices	= new HashMap<Class, TrackedService>();
	final List<TrackedBundle>			bundles			= new CopyOnWriteArrayList<>();
	final Thread						thread			= new Thread(this::run, "aggregate-state");
	final Object						lock			= new Object();

	volatile boolean					closed			= false;

	class TrackedService {

		final Class									serviceType;
		final Set<TrackedBundle>					usedOffers		= new HashSet<>();
		final Map<Class, ActualTypeRegistration>	registrations	= new HashMap<>();
		final AtomicBoolean							changed			= new AtomicBoolean(true);
		final ServiceTracker						tracker;

		boolean										satisfied		= false;
		int											discovered		= 0;
		int											required		= 0;
		long										lastUpdate		= 0;
		boolean										closed			= false;

		class ActualTypeRegistration {
			Class						actualType;
			final List<TrackedListener>	clients	= new ArrayList<>();
			ServiceRegistration			reg;

			ActualTypeRegistration(Class actualType) {
				this.actualType = actualType;
			}

			void register() {
				assert Thread.currentThread() == thread;
				if (reg != null)
					return;

				System.out.println("registering " + actualType);
				try {
					Object instance = getInstance(actualType);
					this.reg = context.registerService(actualType, instance, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			void unregister() {
				assert Thread.currentThread() == thread;
				if (reg == null)
					return;
				reg.unregister();
				reg = null;
				System.out.println("unregistering " + actualType);
			}

			private Object getInstance(Class actual) throws InstantiationException, IllegalAccessException {
				Object instance;
				if (actual.isInterface()) {
					instance = Proxy.newProxyInstance(actual.getClassLoader(), new Class[] { actual },
							(p, m, a) -> {
								throw new UnsupportedOperationException(
										"This was registered as an indication of the aggregate state for " + serviceType
												+ ". It does not support any impl.");
							});
				} else
					instance = actual.newInstance();
				return instance;
			}
		}

		TrackedService(Class serviceType) {
			this.serviceType = serviceType;

			for (TrackedBundle tb : bundles) {
				int n = tb.getOfferedServices(serviceType);
				if (n > 0) {
					usedOffers.add(tb);
					required += n;
				}
			}

			int adjust = Integer.getInteger("aggregate." + serviceType.getName(), 0);
			required += adjust;

			this.tracker = new ServiceTracker(context, this.serviceType, null) {
				@Override
				public Object addingService(ServiceReference reference) {
					synchronized (lock) {
						discovered++;
						changed();
					}
					return reference;
				}

				@Override
				public void removedService(ServiceReference reference, Object service) {
					synchronized (lock) {
						discovered--;
						changed();
					}
				}
			};
			this.tracker.open();
		}

		void update(long now) {
			if (!changed.get())
				return;

			assert Thread.currentThread() == thread;

			while (changed.getAndSet(false)) {
				System.out.println("updating");
				List<ActualTypeRegistration> unregister = new ArrayList<>();
				List<ActualTypeRegistration> register = new ArrayList<>();

				synchronized (lock) {

					registrations.values().removeIf(r -> {
						if (r.clients.isEmpty()) {
							unregister.add(r);
							return true;
						} else
							return false;
					});

					if (registrations.isEmpty()) {
						System.out.println("removing " + this);
						trackedServices.remove(serviceType);
						satisfied = false;
						closed = true;
					} else {
						boolean desiredState = discovered >= required;
						if (satisfied != desiredState) {
							if (desiredState) {
								long passed = now - lastUpdate;
								if (passed > TimeUnit.MILLISECONDS.toNanos(300))
									return;
								else
									register.addAll(registrations.values());
							} else {
								unregister.addAll(registrations.values());
							}
						}
						satisfied = desiredState;
					}
				}

				register.forEach(ActualTypeRegistration::register);
				unregister.forEach(ActualTypeRegistration::unregister);
			}
		}

		synchronized void add(TrackedListener ts) {
			ActualTypeRegistration atr = registrations.computeIfAbsent(ts.actualType, ActualTypeRegistration::new);
			atr.clients.add(ts);
			changed();
		}

		synchronized void remove(TrackedListener ts) {
			ActualTypeRegistration atr = registrations.get(ts.actualType);
			atr.clients.remove(ts);
			changed();
		}

		synchronized void add(TrackedBundle tb) {
			int n = tb.getOfferedServices(serviceType);
			if (n > 0) {
				usedOffers.add(tb);
				required += n;
				lastUpdate = System.nanoTime();
				changed();
			}
		}

		synchronized void remove(TrackedBundle tb) {
			if (usedOffers.remove(tb)) {
				required -= tb.getOfferedServices(serviceType);
				lastUpdate = System.nanoTime();
				changed();
			}
		}

		private void changed() {
			this.lastUpdate = System.nanoTime();
			this.changed.set(true);
		}

		synchronized boolean isEmpty() {
			return registrations.isEmpty();
		}

		void close() {

		}

		@Override
		public String toString() {
			return "TrackedService [serviceType=" + serviceType + ", clients=" + registrations.size()
					+ ", registrations="
					+ registrations.size() + ", satisfied=" + satisfied + ", discovered=" + discovered + ", required="
					+ required + "]";
		}

	}

	AggregateState(BundleContext context) {
		this.context = context;
		thread.start();
	}

	@Override
	public void close() throws Exception {
		synchronized (lock) {
			if (closed)
				return;
			closed = true;
		}
		trackedServices.values().forEach(TrackedService::close);
		thread.interrupt();
		thread.join(10_000);
	}

	AutoCloseable add(Bundle bundle) {
		synchronized (lock) {
			TrackedBundle tb = TrackedBundle.create(bundle);
			if (tb == null)
				return null;
			bundles.add(tb);
			trackedServices.values().forEach(ts -> ts.add(tb));
			return () -> remove(tb);
		}
	}

	private void remove(TrackedBundle tb) {
		synchronized (lock) {
			bundles.remove(tb);
			trackedServices.values().forEach(ts -> ts.remove(tb));
		}
	}

	AutoCloseable add(ListenerInfo li) {
		synchronized (lock) {
			TrackedListener tl = TrackedListener.create(li);
			if (tl == null)
				return null;

			TrackedService trackedService = trackedServices.computeIfAbsent(tl.serviceType, TrackedService::new);
			trackedService.add(tl);
			return () -> {
				synchronized (lock) {
					trackedService.remove(tl);
				}
			};
		}
	}

	void run() {
		try {
			while (true)
				try {
					long now = System.nanoTime();
					List<TrackedService> l = new ArrayList<>(trackedServices.values());
					l.forEach(ts -> ts.update(now));
					Thread.sleep(100);
				} catch (InterruptedException e) {
					thread.interrupt();
					return;
				} catch (Exception e) {
					e.printStackTrace();
				}
		} finally {
			System.out.println("exit " + this);
		}
	}

}
