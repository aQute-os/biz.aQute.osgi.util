package biz.aQute.mqtt.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.osgi.dto.DTO;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import biz.aQute.broker.api.Broker;
import biz.aQute.broker.api.Topic;

public class BrokerTest {
	static LaunchpadBuilder			builder	= new LaunchpadBuilder().bndrun("basic.bndrun");
	static ScheduledExecutorService	ses		= Executors.newScheduledThreadPool(10);
	static Random					random	= new Random();
	@Service
	ConfigurationAdmin				admin;

	final CountDownLatch			latch	= new CountDownLatch(50);

	static public class Event extends DTO {
		public long		timestamp;
		public String	from;
		public String	name;
		public double	value;
	}

	class Backend {
		final List<Event>					events			= new CopyOnWriteArrayList<>();
		final Map<String, Boolean>			localDevices	= new HashMap<>();
		final Broker						broker;
		final Closeable						subscription;
		final Map<String, Topic<Command>>	ids				= new ConcurrentHashMap<>();
		final ScheduledFuture<?>			schedule;

		Backend(Broker broker) {
			this.broker = broker;
			subscription = broker.subscribe(this::receive, Event.class, 0, "backend");
			schedule = ses.scheduleAtFixedRate(this::tick, 100, 100, TimeUnit.MILLISECONDS);
		}

		public void receive(Event event) {
			System.out.println("backend received " + event);
			events.add(event);
			ids.computeIfAbsent("device/" + event.from, topic -> broker.topic(topic, false, 0, Command.class));
		}

		public void tick() {
			if (events.size() > 10) {
				Command command = new Command();
				command.action = Action.STOP;
				ids.values().removeIf(topic -> {
					try {
						topic.publish(command);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return true;
				});
			}
			latch.countDown();
		}

		void close() throws IOException {
			subscription.close();
			schedule.cancel(true);
		}

	}

	enum Action {
		START, STOP, HIGH, LOW;
	};

	static public class Command extends DTO {
		public Action	action;
		public double	argument;
	}

	class LocalDevice {
		final String				id;
		final Topic<Event>			topic;
		final Broker				broker;
		final Closeable				subscribe;
		final AtomicInteger			errors	= new AtomicInteger();
		final ScheduledFuture<?>	schedule;
		volatile boolean			on		= true;
		volatile double				low		= 0;
		volatile double				high	= 1;

		LocalDevice(String id, Broker broker, int period) {
			this.id = id;
			this.broker = broker;
			this.topic = broker.topic("backend", false, 0, Event.class);
			this.subscribe = broker.subscribe(this::receive, Command.class, 0, "device/" + id);
			schedule = ses.scheduleAtFixedRate(this::tick, 200, 200, TimeUnit.MILLISECONDS);
		}

		synchronized void tick() {
			try {
				if (on) {
					Event event = new Event();
					event.from = id;
					event.timestamp = System.currentTimeMillis();
					event.value = random.nextDouble() % (high - low) + low;
					topic.publish(event);
				}
			} catch (Exception e) {
				e.printStackTrace();
				errors.incrementAndGet();
			}
		}

		private <T> void receive(Command command) {
			System.out.println("device " + id + " received " + command);

			switch (command.action) {
			case HIGH:
				this.high = command.argument;
				break;
			case LOW:
				this.low = command.argument;
				break;
			case START:
				on = true;
				break;
			case STOP:
				on = false;
				break;
			default:
				break;
			}
		}

		public void close() throws IOException {
			subscribe.close();
			schedule.cancel(true);
		}
	}

	@Test
	public void simple() throws Exception {
		try (Launchpad lp = builder.create().inject(this).debug()) {

			update("biz.aQute.mqtt.moquette.server", //
					"host", "localhost", //
					"allow.anonymous", true, //
					"port", 2883);

			update("biz.aQute.mqtt.paho.client", //
					"uri", "tcp://clientid@localhost:2883", //
					"name", "broker");

			
			Optional<Broker> t = lp.waitForService(Broker.class, 60_000);
			
			lp.report();
			
			assertThat(t).isPresent();

			Broker broker = t.get();

			Backend backend = new Backend(broker);

			LocalDevice a = new LocalDevice("D-000A", broker, 100);
			LocalDevice b = new LocalDevice("D-000B", broker, 200);

			latch.await();

			assertThat(a.on).isFalse();
			assertThat(b.on).isFalse();
			assertThat(backend.events).hasSizeGreaterThan(10);

			a.close();
			b.close();
			backend.close();
		}
	}

	private void update(String pid, Object... kvs) throws IOException {
		Configuration configuration = admin.getConfiguration(pid, "?");
		Hashtable<String, Object> properties = new Hashtable<>();
		for (int i = 0; i < kvs.length; i += 2) {
			String key = kvs[i].toString();
			Object value = kvs[i + 1];
			properties.put(key, value);
		}
		configuration.update(properties);
	}

}
