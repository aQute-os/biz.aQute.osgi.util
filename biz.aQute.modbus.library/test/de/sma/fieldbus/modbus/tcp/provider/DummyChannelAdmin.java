package de.sma.fieldbus.modbus.tcp.provider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import org.junit.Assert;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import de.sma.async.channel.api.AsyncChannel;
import de.sma.async.channel.api.AsyncChannelAdmin;
import de.sma.async.channel.api.NonBlockingChannelHolder;
import de.sma.async.channel.api.SocketChannelHolder;
import de.sma.iguana.promises.util.ThreadSwitcher;
import de.sma.iguana.test.util.TraceParser.SequenceMatcher;

public class DummyChannelAdmin extends Assert implements AsyncChannelAdmin {

	final ThreadSwitcher runInMainThread;
	public InetSocketAddress address;
	public SequenceMatcher traceRequest;
	
	public DummyChannelAdmin(ThreadSwitcher runInMainThread) {
		this.runInMainThread = runInMainThread;
	}

	@Override
	public <T extends SelectableChannel & ReadableByteChannel & WritableByteChannel> AsyncChannel<T> createChannel(
			NonBlockingChannelHolder<T> channelHolder,
			ToIntFunction<ByteBuffer> defragment, int readBufferSize)
			throws Exception {

		assertTrue(channelHolder instanceof SocketChannelHolder);
		SocketChannelHolder sch = (SocketChannelHolder) channelHolder;
		InetSocketAddress address = sch.getAddress();
		if ( this.address != null)
			assertEquals(this.address, address);

		return new AsyncChannel<T>() {

			private Consumer<ByteBuffer> processor;

			@Override
			public void close() throws IOException {
			}

			@Override
			public boolean isConnected() throws IOException {
				return false;
			}

			@Override
			public Promise<ByteBuffer> receive(long timeout) throws Exception {
				fail("Receive should not be called");
				return null;
			}

			@Override
			public Promise<Void> transmit(ByteBuffer data, long timeout)
					throws Exception {
				Deferred<Void> deferred = new Deferred<>();

				if (processor != null) {
					runInMainThread.async(() -> {
						try {
							ByteBuffer response = traceRequest
									.nextMatch(data);
							processor.accept(response);
							deferred.resolve(null);
						} catch (Throwable e) {
							deferred.fail(e);
						}
					});

				}
				return deferred.getPromise();
			}

			@Override
			public T currentChannel() throws Exception {
				fail("Dont touch my underlying channel!");
				return null;
			}

			@Override
			public void onReceive(Consumer<ByteBuffer> processor) {
				this.processor = processor;
			}

		};
	}

}
