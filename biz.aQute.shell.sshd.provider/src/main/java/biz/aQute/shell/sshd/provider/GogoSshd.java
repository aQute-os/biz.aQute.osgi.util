package biz.aQute.shell.sshd.provider;

import java.io.EOFException;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import biz.aQute.authentication.api.Authenticator;
import biz.aQute.shell.sshd.config.SshdConfig;

/**
 * Starts a Gogo shell accessible via SSH.
 */

@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL, configurationPid = SshdConfig.PID)
public class GogoSshd {
	final static Logger		logger	= LoggerFactory.getLogger(GogoSshd.class);
	final static byte[]		crlf	= new byte[] { 10, 13 };

	final SshServer			sshd	= SshServer.setUpDefaultServer();
	final CommandProcessor	processor;
	final SshdConfig		config;
	final BundleContext		context;

	String					userid;

	@Activate
	public GogoSshd(BundleContext context, @Reference CommandProcessor processor, SshdConfig config)
			throws IOException {
		this.context = context;
		this.processor = processor;
		this.config = config;
		sshd.setPort(config.port());
		sshd.setHost(config.address());

		String keyPath = config.privateKeyPath();
		File keyFile;
		if (keyPath == null) {
			keyFile = context.getDataFile("host.ser");
		} else {
			keyFile = IO.getFile(keyPath);
		}

		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(keyFile.toPath()));
		if (!config.optionalAuthentication()) {
//			sshd.setHostBasedAuthenticator(this::authenticate);
			sshd.setPasswordAuthenticator(this::authenticate);
		}

		sshd.setCommandFactory((s, c) -> getCommand(processor));
		sshd.setShellFactory((ChannelSession channel) -> getCommand(processor));
		sshd.start();
	}

	private Command getCommand(CommandProcessor processor) {
		return new Command() {

			OutputStream	out;
			InputStream		in;
			ExitCallback	callback;
			OutputStream	err;
			CommandSession	session;
			private Thread	thread;

			@Override
			public void start(ChannelSession channel, Environment env) throws IOException {
				int w = getInt(env.getEnv().get(Environment.ENV_COLUMNS), 80);
				int h = getInt(env.getEnv().get(Environment.ENV_LINES), 40);
				String type = env.getEnv().getOrDefault(Environment.ENV_TERM, "ansi");

				AnsiFilter console = new AnsiFilter(w, h, in, out, type, StandardCharsets.UTF_8);

				session = processor.createSession(in, out, err);
				session.put("_env", env.getEnv());
				session.put("history", (Function) (ses, args) -> {
					List<String> r = new ArrayList<>();
					for (String s : console.getHistory()) {
						r.add(String.format("%03d: %s", r.size(), s));
					}
					Collections.reverse(r);
					return r;
				});

				session.put("exit", (Function) (ses, args) -> {
					logger.info("exiting {} {}", userid, ses);
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
				thread = new Thread() {
					public void run() {
						try {
							while (!Thread.currentThread().isInterrupted())
								try {

									String line = console.readline(getPrompt());
									if (line == null)
										return;
									if (!Strings.trim(line).isEmpty()) {
										console.write("\n");
										try {
											Object execute = session.execute(line);
											if (execute != null) {
												String s = session.format(execute, Converter.INSPECT).toString();
												console.write(s);
											}
										} catch (Exception e) {
											console.write(e.getMessage());
										}
									}
									console.write("\n");
								} catch (EOFException e) {
									// ignore
									return;
								} catch (Exception e) {
									callback.onExit(0, e.toString());
								}
						} finally {
							try {
								console.flush();
							} catch (IOException e) {
								// ignore
							}
							session.close();
							callback.onExit(0);
							logger.info("quiting thread");
						}
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
				};
				thread.start();
			}

			private int getInt(String value, int deflt) {
				try {
					return Integer.parseInt(value);
				} catch (Exception e) {
					return deflt;
				}
			}

			@Override
			public void destroy(ChannelSession channel) throws Exception {
				session.close();
				thread.interrupt();
			}

			@Override
			public void setOutputStream(OutputStream out) {
				this.out = new FilterOutputStream(out) {
					public void write(int b) throws IOException {
						if (b == '\n') {
							out.write(crlf);
						} else {
							out.write(b);
						}
					}
				};
			}

			@Override
			public void setInputStream(InputStream in) {
				this.in = in;
			}

			@Override
			public void setExitCallback(ExitCallback callback) {
				this.callback = callback;
			}

			@Override
			public void setErrorStream(OutputStream err) {
				this.err = err;
			}

		};
	}

	@Deactivate
	void deactivate() throws IOException {
		sshd.stop();
		sshd.close();
	}

	/*
	 * This method can run into the problem that the Authentication class is not
	 * present. In that case authentication will fail
	 */
	boolean authenticate(String user, String password, ServerSession serversession) {
		try {
			ServiceReference<Authenticator> ref = context.getServiceReference(Authenticator.class);
			if (ref == null) {
				return false;
			}

			Authenticator authenticator = context.getService(ref);
			if (authenticator == null)
				return false;

			try {
				Map<String, Object> map = new HashMap<>();
				map.put(Authenticator.BASIC_SOURCE,
						"socket:/" + serversession.getLocalAddress() + serversession.getRemoteAddress());
				map.put(Authenticator.BASIC_SOURCE_USERID, user);
				map.put(Authenticator.BASIC_SOURCE_PASSWORD, password);

				userid = authenticator.authenticate(map, Authenticator.BASIC_SOURCE);
				return userid != null;
			} finally {
				context.ungetService(ref);
			}
		} catch (Error e) {
			return false;
		}
	}

	boolean authenticate(
			ServerSession session, String username,
			PublicKey clientHostKey, String clientHostName, String clientUsername, List<X509Certificate> certificates) {

		System.out.printf("host based authentication 5s %s %s %s %s %s\n", session, username, clientHostKey, clientHostName, clientUsername);
		return false;
	}

}
