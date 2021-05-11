package biz.aQute.shell.sshd.provider;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.osgi.framework.BundleContext;

import aQute.lib.strings.Strings;

class CommandSessionHandler implements Closeable, Runnable {

	static final String		NEWLINE	= "\r\n";
	final CommandSession	session;
	final AnsiFilter		console;
	final ExitCallback		callback;
	final Thread			thread;
	final AtomicBoolean		quit	= new AtomicBoolean();

	CommandSessionHandler(BundleContext context, String username, Map<String, String> env,
			InputStream in, OutputStream out, OutputStream err, CommandProcessor processor, ExitCallback callback)
			throws Exception {
		this.thread = new Thread(this, "sshd-gogo-" + username);
		this.callback = callback;
		int w = getInt(env.get(Environment.ENV_COLUMNS), 80);
		int h = getInt(env.get(Environment.ENV_LINES), 40);
		String term = env.getOrDefault(Environment.ENV_TERM, "none");

		console = new AnsiFilter(w, h, in, out, term, StandardCharsets.UTF_8);

		session = processor.createSession(in, out, err);
		session.put(".context",context);
		session.put(".system", System.class);
		session.put(".env", env);
		session.put("history", (Function) (ses, args) -> {
			List<String> r = new ArrayList<>();
			for (String s : console.getHistory()) {
				r.add(String.format("%03d: %s", r.size(), s));
			}
			Collections.reverse(r);
			return r;
		});

		session.put("exit", (Function) (ses, args) -> {
			AbstractGogoSshd.logger.info("exiting {} {}", ses,args);
			switch (args.size()) {
			case 0:
				callback.onExit(0);
				break;

			case 1:
				callback.onExit(0, args.get(0).toString());
				break;

			default:
				if ("\\d+".matches(args.get(0).toString())) {
					callback.onExit(Integer.parseInt(args.remove(0).toString()), args.toString());
				}
				break;

			}
			return null;
		});

		session.execute("addcommand context ${.context}");
		session.execute("addcommand context ${.context} (${.context} class)");
		session.execute("addcommand system ${.system}");

		this.thread.setDaemon(true);
		this.thread.start();
	}

	public void run() {
		try {
			while (!thread.isInterrupted())
				try {

					String line = console.readline(getPrompt());
					if (line == null) {
						callback.onExit(0);
						return;
					}
					boolean wasnl=false;
					if ( console.echo ) {
						console.writeln();
					}
					if (!Strings.trim(line).isEmpty()) {
						try {
							Object execute = execute(line);
							if (execute != null) {
								String s = session.format(execute, Converter.INSPECT).toString();
								console.write(s);
								wasnl = s.endsWith("\n");
							}
						} catch (Exception e) {
							session.put("exception", e);
							if ( e.getMessage() == null || e.getMessage().trim().isEmpty())
								console.write(e.getClass().getName());
							else
								console.write(e.getMessage());
							console.writeln();
							wasnl=true;
						}
						if (!wasnl)
							console.writeln();
					}
					console.flush();
				} catch (Throwable e) {
					// ignore
					return;
				}
		} finally {
			try {
				console.flush();
			} catch (IOException e) {
				// ignore
			}
			session.close();
			AbstractGogoSshd.logger.info("quiting thread");
		}
	}

	protected Object execute(String line) throws Exception {
		return session.execute(line);
	}

	private String getPrompt() {
		Object o = session.get("PS1");
		if (o == null)
			return "$ ";

		if (o instanceof Function) {
			try {
				return ((Function) o).execute(session, Collections.emptyList()).toString();
			} catch (Exception e) {
				return e.getMessage() + " ";
			}
		}
		return o.toString();
	}

	private int getInt(String value, int deflt) {
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return deflt;
		}
	}

	@Override
	public void close() throws IOException {
		if (quit.getAndSet(true) == false) {
			thread.interrupt();
			try {
				thread.join(5000);
			} catch (InterruptedException e) {
				thread.interrupt();
				throw new RuntimeException(e);
			}
		}
	}

	public AnsiFilter getANSI() {
		return console;
	}

}
