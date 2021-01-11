package biz.aQute.shell.sshd.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.session.ServerSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import aQute.lib.strings.Strings;
import biz.aQute.authentication.api.Authenticator;
import biz.aQute.authorization.api.Authority;
import biz.aQute.authorization.api.AuthorityAdmin;
import biz.aQute.shell.sshd.config.SshdConfig;

@Designate(ocd = SshdConfig.class, factory = true)
@Component
public class GogoSshdSecure extends AbstractGogoSshd {

	final Authenticator authenticator;
	final Authority authority;
	final AuthorityAdmin admin;
	final Map<ServerSession, String> users = Collections.synchronizedMap(new IdentityHashMap<ServerSession, String>());
	final String permission;

	@Activate
	public GogoSshdSecure(BundleContext context, @Reference CommandProcessor processor,
			@Reference Authenticator authenticator, @Reference Authority authority, @Reference AuthorityAdmin admin,
			SshdConfig config) throws IOException {
		super(context, processor, config.hostkey(), config.address(), config.port());
		this.authenticator = authenticator;
		this.authority = authority;
		this.admin = admin;
		this.permission = config.permission();
		if (config.passwords()) {
			sshd.setPasswordAuthenticator(this::authenticate);
		}
		sshd.setPublickeyAuthenticator(this::authenticate);
		open();
	}

	@Override
	protected CommandSessionHandler getCommandSessionHandler(BundleContext context, ChannelSession channel,
			Environment env, InputStream in, OutputStream out, OutputStream err, CommandProcessor processor2,
			ExitCallback callback) throws Exception {

		String user = users.get(channel.getServerSession());

		return new CommandSessionHandler(context, channel, env, in, out, err, processor, callback) {

			@Override
			public void run() {
				try {
					admin.call(user, () -> {
						super.run();
						return null;
					});
				} catch (Exception e) {
					logger.warn("gogo command failed {}", e, e);
				}
			}

			@Override
			protected Object execute(String line) throws Exception {
				assert Thread.currentThread() == this.thread;

				List<String> split = Strings.split("\\s+", line);
				String cmd = split.remove(0);

				authority.checkPermission(permission, cmd);

				return super.execute(line);
			}
		};
	}

	private boolean authenticate(String username, String password, ServerSession serverSession) {
		Map<String, Object> map = new HashMap<>();
		map.put(Authenticator.BASIC_SOURCE_USERID, username);
		map.put(Authenticator.BASIC_SOURCE_PASSWORD, password);

		String authenticate = authenticator.authenticate(map, Authenticator.BASIC_SOURCE);
		if (authenticate == null)
			return false;
		users.put(serverSession, authenticate);
		return true;
	}

	private boolean authenticate(String username, PublicKey key, ServerSession serverSession) {
		Map<String, Object> map = new HashMap<>();
		map.put(Authenticator.BASIC_SOURCE_USERID, username);
		map.put(Authenticator.BASIC_SOURCE_PASSWORD, key);
		String authenticate = authenticator.authenticate(map, Authenticator.BASIC_SOURCE);
		if (authenticate == null)
			return false;
		users.put(serverSession, authenticate);
		return true;
	}

	@Deactivate
	void deactivate() throws IOException {
		super.deactivate();
	}

}
