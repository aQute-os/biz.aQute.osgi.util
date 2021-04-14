package biz.aQute.shell.sshd.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.io.IO;

abstract class AbstractGogoSshd {
	final static Logger logger = LoggerFactory.getLogger("GogoSshd");

	final SshServer sshd;
	final CommandProcessor processor;
	final BundleContext context;

	volatile int port;

	AbstractGogoSshd(BundleContext context, CommandProcessor processor, String keypath, String host, int port)
			throws IOException {
		this.context = context;
		this.processor = processor;
		this.sshd = SshServer.setUpDefaultServer();

		this.sshd.setPort(port);
		this.sshd.setHost(host);

		this.sshd.setCommandFactory((s, c) -> getCommand(processor));

		File keyFile = IO.getFile(keypath);
		keyFile.getParentFile().mkdirs();
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(keyFile.toPath()));
		this.sshd.setShellFactory((ChannelSession channel) -> getCommand(processor));
	}

	void open() throws IOException {
		sshd.start();
		port = sshd.getPort();
		logger.info("SshServer opened: {}", sshd.toString());
	}

	private Command getCommand(CommandProcessor processor) {

		return new Command() {

			OutputStream out;
			OutputStream err;
			InputStream in;
			ExitCallback callback;
			CommandSessionHandler session;

			@Override
			public void start(ChannelSession channel, Environment env) throws IOException {
				try {
					session = getCommandSessionHandler(context, channel, env, in, out, err, processor, callback);
				} catch (Exception e) {
					logger.error("failed to create a session {}", e, e);
					throw new RuntimeException(e);
				}
			}

			@Override
			public void destroy(ChannelSession channel) throws Exception {
				session.close();
			}

			@Override
			public void setOutputStream(OutputStream out) {
				this.out = out;
			}

			@Override
			public void setInputStream(InputStream in) {
				this.in = in;
			}

			@Override
			public void setErrorStream(OutputStream err) {
				this.err = err;
			}

			@Override
			public void setExitCallback(ExitCallback callback) {
				this.callback = callback;
			}

		};
	}

	@Deactivate
	void deactivate() throws IOException {
		sshd.stop(true);
		sshd.close();
	}

	protected abstract CommandSessionHandler getCommandSessionHandler(BundleContext context, ChannelSession channel,
			Environment env, InputStream in, OutputStream out, OutputStream err, CommandProcessor processor2,
			ExitCallback callback) throws Exception;

}
