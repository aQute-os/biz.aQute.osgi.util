package biz.aQute.shell.sshd.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.session.ServerSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import biz.aQute.shell.sshd.config.SshdConfigInsecure;

@Designate(ocd = SshdConfigInsecure.class, factory = true)
@Component(configurationPid = SshdConfigInsecure.PID )
public class GogoSshdInsecure extends AbstractGogoSshd {

	@Activate
	public GogoSshdInsecure(BundleContext context, @Reference CommandProcessor processor, SshdConfigInsecure config)
			throws IOException {
		super(context, processor, config.hostkey(), "localhost", config.port());
		logger.warn("starting insecure ssh server on port localhost:{}", config.port());

		sshd.setPasswordAuthenticator(new PasswordAuthenticator() {

			@Override
			public boolean authenticate(String username, String password, ServerSession session)
					throws PasswordChangeRequiredException, AsyncAuthException {
				if ( config.password().equals("*"))
					return true;
				return config.user().equals( username) && config.password().equals(password);
			}
		});
		open();
	}
	@Deactivate
	void deactivate() throws IOException {
		super.deactivate();
	}

	@Override
	protected CommandSessionHandler getCommandSessionHandler(BundleContext context, ChannelSession channel,
			Environment env, InputStream in, OutputStream out, OutputStream err, CommandProcessor processor,
			ExitCallback callback) throws Exception {
		return new CommandSessionHandler(context, channel.getSession().getUsername(), env.getEnv(), in, out, err, processor, callback);
	}


}
