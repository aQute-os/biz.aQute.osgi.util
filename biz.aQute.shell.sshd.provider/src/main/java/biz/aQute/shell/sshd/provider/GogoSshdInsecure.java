package biz.aQute.shell.sshd.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.channel.ChannelSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import biz.aQute.shell.sshd.config.SshdConfig;
import biz.aQute.shell.sshd.config.SshdConfigInsecure;

@Designate(ocd = SshdConfig.class, factory = true)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = SshdConfigInsecure.PID)
public class GogoSshdInsecure extends AbstractGogoSshd {

	@Activate
	public GogoSshdInsecure(BundleContext context, @Reference CommandProcessor processor, SshdConfigInsecure config)
			throws IOException {
		super(context,processor, config.hostkey(), "localhost", config.port());
		logger.warn("starting insecure ssh server on port localhost:{}", config.port());

		sshd.setPasswordAuthenticator(AcceptAllPasswordAuthenticator.INSTANCE);
		sshd.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
		open();
	}

	@Override
	protected CommandSessionHandler getCommandSessionHandler(BundleContext context, ChannelSession channel, Environment env, InputStream in,
			OutputStream out, OutputStream err, CommandProcessor processor, ExitCallback callback) throws Exception {
		return new CommandSessionHandler(context,channel, env, in, out, err, processor, callback);
	}
}
