package biz.aQute.mqtt.paho.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import aQute.lib.io.IO;

class NetProxy extends Thread {
	final ServerSocket	server;
	final int			port;
	final String		host;
	volatile boolean	enabled	= true;

	class Worker extends Thread implements Closeable {
		final InputStream	in;
		final OutputStream	out;
		final Session		session;

		Worker(Session session, InputStream in, OutputStream out) {
			this.session = session;
			this.in = in;
			this.out = out;
			start();
		}

		public void run() {

			try {
				while (!isInterrupted())
					try {
						if (!enabled) {
							Thread.sleep(100);
						} else {
							int v = in.read();
							if (v < 0)
								return;

							out.write(v);
						}
					} catch (Exception e) {
						if (!isInterrupted()) {
							e.printStackTrace();
						}
						session.close();
						return;
					}
			} finally {
				IO.close(session);
			}

		}

		@Override
		public void close() throws IOException {
			System.out.println("closing worker");
			interrupt();
			IO.close(in);
			IO.close(out);
		}

	}

	class Session implements AutoCloseable {
		final Socket	local;
		final Socket	remote;
		final Worker	ctos;
		final Worker	stoc;
		
		Session(Socket local) throws IOException {
			this.local = local;
			this.remote = new Socket(host, port);
			ctos = new Worker(this,local.getInputStream(), remote.getOutputStream());
			stoc = new Worker(this,remote.getInputStream(), local.getOutputStream());
		}
		
		public void close() {
			System.out.println("closing session");
			ctos.interrupt();
			stoc.interrupt();
			IO.close(local);
			IO.close(remote);
		}
	}

	NetProxy(String host, int port) throws IOException {
		this.port = port;
		this.host = host;
		this.server = new ServerSocket(0);
		this.start();
	}

	public void run() {
		List<Session>	sessions = new ArrayList<>();
		while (!isInterrupted()) {
			try {
				Socket local = server.accept();
				
				System.out.println("got a connection from " + local);
				Session s = new Session(local);
				sessions.add(s);
				
			} catch (Exception e) {
				System.out.println("server failed in netproxy");
				e.printStackTrace();
			}
		}
		System.out.println("netproxy done");
		sessions.forEach( Session::close);
	}

	public void close() {
		interrupt();
		IO.close(server);
	}

	public int getPort() {
		return server.getLocalPort();
	}

	public void enable(boolean enabled) {
		System.out.println("enable " + enabled);
		this.enabled = enabled;
	}
}