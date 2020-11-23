package de.sma.fieldbus.modbus.tcp.provider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import de.sma.async.channel.api.AsyncChannel;
import de.sma.async.channel.api.AsyncChannelAdmin;
import de.sma.async.channel.api.NonBlockingChannelHolder;
import de.sma.async.channel.api.SocketChannelHolder;
import de.sma.comlayer.api.PacketProcessingResult;
import de.sma.comlayer.api.Route;
import de.sma.fieldbus.modbus.core.provider.ADU.WordOrder;
import de.sma.fieldbus.modbus.core.provider.MBAP;
import de.sma.fieldbus.modbus.master.mapping.provider.ModbusDriver;
import de.sma.iguana.promises.util.ThreadSwitcher;
import de.sma.iguana.test.util.DummyComDispatcher;
import de.sma.iguana.test.util.TraceParser;
import de.sma.iguana.test.util.TraceParser.SequenceMatcher;

public class ModbusTCPTest extends Assert {
	ScheduledExecutorService	es				= Executors
			.newScheduledThreadPool(10);
	ThreadSwitcher				runInMainThread	= new ThreadSwitcher();
	Semaphore					semaphore		= new Semaphore(0);
	final MBAP					mbap			= new MBAP(WordOrder.bigEndian,
			null);

	static class Expectations {
		InetSocketAddress		address;
		SequenceMatcher			traceRequest;
		Route					to;
		public SequenceMatcher	routed;
		public SequenceMatcher	traceRouted;
		public Route			from;
	}

	static Expectations expectations = new Expectations();

	@After
	public void close() {
		es.shutdownNow();
	}

	AsyncChannelAdmin	channelAdmin	= new AsyncChannelAdmin() {
											@Override
											public <T extends SelectableChannel & ReadableByteChannel & WritableByteChannel> AsyncChannel<T> createChannel(
													NonBlockingChannelHolder<T> channelHolder,
													ToIntFunction<ByteBuffer> defragment,
													int readBufferSize)
													throws Exception {

												assertTrue(
														channelHolder instanceof SocketChannelHolder);
												SocketChannelHolder sch = (SocketChannelHolder) channelHolder;
												InetSocketAddress address = sch
														.getAddress();
												assertEquals(
														expectations.address,
														address);

												return new AsyncChannel<T>() {

																						private Consumer<ByteBuffer> processor;

																						@Override
																						public void close()
																								throws IOException {
																						}

																						@Override
																						public boolean isConnected()
																								throws IOException {
																							return false;
																						}

																						@Override
																						public Promise<ByteBuffer> receive(
																								long timeout)
																								throws Exception {
																							fail("Receive should not be called");
																							return null;
																						}

																						@Override
																						public Promise<Void> transmit(
																								ByteBuffer data,
																								long timeout)
																								throws Exception {
																							Deferred<Void> deferred = new Deferred<>();

																							if (processor != null) {
																								es.schedule(
																										() -> {
																																				try {
																																					ByteBuffer response = expectations.traceRequest
																																							.nextMatch(
																																									data);
																																					processor
																																							.accept(response);
																																					deferred.resolve(
																																							null);
																																				} catch (Throwable e) {
																																					deferred.fail(
																																							e);
																																				}
																																			},
																										200,
																										TimeUnit.MILLISECONDS);

																							}
																							return deferred
																									.getPromise();
																						}

																						@Override
																						public T currentChannel()
																								throws Exception {
																							fail("Dont touch my underlying channel!");
																							return null;
																						}

																						@Override
																						public void onReceive(
																								Consumer<ByteBuffer> processor) {
																							this.processor = processor;
																						}

																					};
											}
										};


	@Test
	public void testSimple() throws Exception {

		TraceParser tp = new TraceParser(
				ModbusTCPTest.class.getResourceAsStream("simple-trace.txt"));

		DummyComDispatcher dummyComDispatcher = new DummyComDispatcher(
				runInMainThread);
		dummyComDispatcher.matcher = tp.getMatcher("simple.routed");

		ModbusTCP mtcp = new ModbusTCP();
		mtcp.channelAdmin = channelAdmin;
		mtcp.dispatcher = dummyComDispatcher;

		expectations.traceRequest = tp.getMatcher("simple.request");
		expectations.traceRouted = tp.getMatcher("simple.routed");
		expectations.address = new InetSocketAddress("172.16.0.27", 502);
		expectations.to = new Route(ModbusDriver.MODBUS_ADU_NODE);
		expectations.from = new Route(
				ModbusTCP.MODBUS_TCP_NODE + ":172.16.0.27:502:1");

		Promise<PacketProcessingResult> p = mtcp.process(
				mbap.readHoldingRegistersRequest(0x4000, 2), null,
				new Route("modbus.tcp:172.16.0.27:502:1"));
		assertEquals(PacketProcessingResult.OK, p.getValue());

		semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS);

		//
		// Run the assertions on the main thread
		//

		runInMainThread.flush();
	}

}
