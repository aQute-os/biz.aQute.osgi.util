package biz.aQute.modbus.ip;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.io.IO;
import biz.aQute.modbus.api.MessagingProtocol;
import biz.aQute.modbus.api.PDU;

public class ModbusTCP extends MessagingProtocol implements Closeable {
	static Logger					logger	= LoggerFactory
			.getLogger(ModbusTCP.class);
	final List<ModbusTCPIncoming>	clients	= new CopyOnWriteArrayList<>();
	final int						port;
	final InetAddress				bindAddr;
	final Thread					thread;

	ServerSocket					server;

	public ModbusTCP(int port, InetAddress bindAddr, boolean bigWordEndian) throws IOException {
		super(bigWordEndian);
		this.thread = new Thread(this::run, bindAddr + ":" + port + (bigWordEndian ? "be" : "le"));
		this.port = port;
		this.bindAddr = bindAddr;
		this.server = bindAddr != null ? new ServerSocket(port, 50, bindAddr) : new ServerSocket(port);
	}

	public int getLocalPort() {
		return this.server.getLocalPort();
	}

	@Override
	public void close() {
		thread.interrupt();
		try {
			thread.join(5000);
		} catch (InterruptedException e) {
			// ignore
		}
		IO.close(this.server);
	}

	class ModbusTCPIncoming extends Thread implements Closeable {
		final Socket channel;

		public ModbusTCPIncoming(Socket channel) {
			this.channel = channel;
		}

		@Override
		public void run() {
			try {
				InputStream in = channel.getInputStream();
				OutputStream out = channel.getOutputStream();
				serve(in, out);
				logger.info("Closing client {}");
			} catch (SocketTimeoutException se) {
				// ignore
			} catch (Throwable t) {
				if (Thread.currentThread().isInterrupted())
					return;
				logger.warn("socket connection {} failed with: {} ", this, t);
				t.printStackTrace();
			} finally {
				logger.info("socket connection {} closed ", this);
				clients.remove(this);
				IO.close(channel);
				System.out.println("Exit thread");
			}
		}

		void read(InputStream in, PDU pdu) throws IOException, InterruptedException {
			while (!Thread.currentThread().isInterrupted()) {

				int read = in.read();
				if (read < 0) {
					logger.info("read -1, quit");
					System.out.println("quiting");
					throw new EOFException();
				}
				pdu.putU8(read);
				if (pdu.position() < 6)
					continue;

				int l = pdu.getU16(4);
				pdu.read(in, l);
				return;
			}
			throw new InterruptedException();
		}

		void serve(InputStream in, OutputStream out) {
			PDU request = getPDU();

			while (!Thread.currentThread().isInterrupted())
				try {
					request.reset();
					read(in, request);
					request.seal();
					System.out.println("rqs " + this + "\n" + request);
					PDU response = accept(request);
					response.position(0);
					System.out.println("rsp " + this + "\n" + response);

					response.write(out);
				} catch (EOFException e) {
					return;
				} catch (IOException e) {
					e.printStackTrace();
					return;
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
		}

		@Override
		public void close() {
			this.interrupt();
		}

		@Override
		public String toString() {
			return "ModbusTCPIncoming[" + channel.getRemoteSocketAddress() + "->" + channel.getLocalPort() + "]";
		}
	}

	void run() {
		try {
			int localPort = getLocalPort();
			while (!thread.isInterrupted())
				try {

					while (!thread.isInterrupted()) {
						Socket channel = server.accept();
						ModbusTCPIncoming client = new ModbusTCPIncoming(channel);
						System.out.println("accept " + channel);
						clients.add(client);
						client.start();
					}

				} catch (Exception e) {
					if (thread.isInterrupted())
						return;
					e.printStackTrace(); // log
					IO.close(server);
					Thread.sleep(1000);
					try {
						open(localPort);
					} catch (IOException ee) {
						// keep ignoring this, we will wait 1 sec between thse
						// tries
					}
				} finally {
					IO.close(server);
				}
		} catch (InterruptedException e) {
			return;
		} finally {
			for (ModbusTCPIncoming client : clients) {
				IO.close(client);
			}

			for (ModbusTCPIncoming client : clients) {
				try {
					client.join(5000);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}

	}

	private void open(int localPort) throws IOException {
		this.server = bindAddr != null ? new ServerSocket(localPort, 50, bindAddr)
				: new ServerSocket(port);
	}

	public void start() {
		thread.start();
	}
}
