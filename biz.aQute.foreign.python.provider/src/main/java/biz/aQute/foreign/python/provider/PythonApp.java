package biz.aQute.foreign.python.provider;

import static biz.aQute.foreign.python.provider.About.logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.Bundle;

import aQute.lib.io.IO;
import aQute.libg.command.Command;

class PythonApp extends Thread {

	final Bundle			bundle;
	final File				storage;
	final File				version;
	volatile Command		command;
	final String			python;
	volatile boolean		closed;
	final CommandProcessor	gogo;
	GogoDTO					gogodto;
	final long				restartDelay;

	// KPI
	volatile int			restarts	= 0;
	boolean					copied		= false;
	volatile int			result;

	public PythonApp(Bundle bundle, String python, CommandProcessor gogo, long restartDelay) throws IOException {
		super("Python " + bundle);

		this.bundle = bundle;
		this.python = System.getProperty("biz.aQute.python.command", python);
		this.gogo = gogo;
		this.restartDelay = restartDelay;
		this.storage = bundle.getDataFile("python");
		this.version = IO.getFile(storage, ".version");
	}

	void open() throws Exception {
		storage.mkdirs();
		if (!storage.isDirectory()) {
			throw new IllegalArgumentException(
					"Cannot create directory " + storage + " to store the python program for " + bundle);
		}

		String newer = bundle.getLastModified() + "";
		if (storage.list().length == 0 || !Objects.equals(newer, IO.collect(version))) {
			System.out.println("updating");
			IO.delete(storage);
			URI base = bundle.getEntry("python/").toURI().normalize();
			Enumeration<URL> entries = bundle.findEntries("python", "*", true);
			while (entries.hasMoreElements()) {
				URL url = entries.nextElement();
				URI entry = url.toURI().normalize();
				String relativized = base.relativize(entry).getPath();
				if (relativized.endsWith("/"))
					continue;
				File file = IO.getFile(storage, relativized);
				file.getParentFile().mkdirs();
				IO.copy(url, file);
			}
			IO.store(newer, version);
			copied = true;
		} else
			System.out.println("using cache");

		start();
	}

	@Override
	public void run() {
		while (!closed) {
			CommandSession session = null;
			try {
				try (GogoDTO gogo = new GogoDTO(this.gogo)) {
					this.gogodto = gogo;
					command = new Command();
					command.add(python);
					command.add("app.py");
					command.setCwd(storage);
					if (closed)
						return;

					logger.debug("starting {}", this);

					gogo.start();
					command.setUseThreadForInput(true);
					LineBreakingStream stderr = new LineBreakingStream(System.err);
					result = command.execute(gogo.getStdin(), gogo.getStdout(), stderr);
					stderr.flush();
					if (closed) {
						logger.debug("executed {} result {}", this, result);
						return;
					} else {
						logger.error("executed {} result {}", this, result);
					}
				}
				restarts++;
				Thread.sleep(restartDelay);
			} catch (InterruptedException e) {
				logger.info("leaving {}", this);
				return;
			} catch (Exception e) {
				logger.error("executed {} result {}", this, e, e);
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e1) {
					return;
				}
			} finally {
				IO.close(session);
			}
		}
	}

	synchronized void close() throws InterruptedException {
		if (!this.closed) {
			this.closed = true;
			this.interrupt();
			if (command != null)
				try {
					this.command.cancel();
				} catch (Exception e) {
					// ignore
				}
			this.join();
		}
	}

	@Override
	public String toString() {
		long stale = -1;
		if (gogodto != null) {
			stale = System.currentTimeMillis() - gogodto.lasttime;
		}
		return "PythonApp [bundle=" + bundle + ", storage=" + storage + ", version=" + version.getName() + ", stale ms="
			+ stale + ", result=" + result + "]";
	}

}
