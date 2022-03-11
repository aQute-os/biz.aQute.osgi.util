package biz.aQute.shell.sshd.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.awaitility.Awaitility;
import org.junit.Test;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.lib.converter.Converter;
import biz.aQute.authentication.api.Authenticator;
import biz.aQute.authorization.api.Authority;
import biz.aQute.authorization.api.AuthorityAdmin;
import biz.aQute.shell.sshd.config.SshdConfig;

public class GogoSshdTest {

	final static LaunchpadBuilder	builder	= new LaunchpadBuilder().bndrun("tester.bndrun").bundles("org.apache.felix.gogo.runtime").debug();

	@Service
	CommandProcessor				cp;
	
	@Test
	public void testSecurity() throws Exception {
		try (Launchpad lp = builder.create().report().inject(this)) {
			Map<String, Object> map = new HashMap<>();
			map.put("passwords", true);
			map.put("permission", "gogo.command:(lsb|bundles)");
			map.put("port", 0);
			SshdConfig config = Converter.cnv(SshdConfig.class, map);
			AtomicBoolean authenticated = new AtomicBoolean();
			AtomicBoolean authorized = new AtomicBoolean();
			AtomicBoolean called = new AtomicBoolean();
			Authority authority = new Authority() {

				@Override
				public boolean hasPermission(String permission, String... arguments) {
					System.out.println("hasPermission " + permission + " " + Arrays.toString(arguments));
					authorized.set(true);
					return true;
				}

				@Override
				public String getUserId() {
					return "foo";
				}

				@Override
				public List<String> getPermissions() {
					return Collections.emptyList();
				}
			};
			AuthorityAdmin admin = new AuthorityAdmin() {

				@Override
				public <T> T call(String userId, Callable<T> protectedTask) throws Exception {
					called.set(true);
					return protectedTask.call();
				}
			};
			Authenticator authenticator = new Authenticator() {

				@Override
				public String authenticate(Map<String, Object> arguments, String... sources) {
					System.out.println(arguments);
					authenticated.set(true);
					return "foo";
				}

				@Override
				public boolean forget(String userid) {
					return false;
				}

			};

			GogoSshdSecure g = new GogoSshdSecure(lp.getBundleContext(),cp, authenticator, authority, admin, config);
			System.out.println("permission " + g.permission);
			
			System.out.println("port " + g.port);
			login(g.port);
			g.deactivate();
			assertThat(authenticated.get()).isTrue();
			assertThat(authorized.get()).isTrue();
			assertThat(called.get()).isTrue();
		}
	}

	private void login(int port) throws Exception {
		try (SshClient client = setupTestClient()) {
			client.start();
			client.setKeyIdentityProvider(null);

			try (final ClientSession session = client.connect("user", "localhost", port).verify(7L, TimeUnit.SECONDS)
					.getSession()) {
				session.addPasswordIdentity("foo");
				session.auth().verify(11L, TimeUnit.SECONDS);

				ByteArrayOutputStream bout = new ByteArrayOutputStream();

				try (final   ChannelShell channel = session.createShellChannel()) {
					channel.setEnv("TERM", "none");
					channel.setOut(bout);
					channel.setErr(bout);

					channel.open().verify(15L, TimeUnit.SECONDS);

					OutputStream in = channel.getInvertedIn();
					in.write("bundles\r".getBytes());
					in.flush();
					Awaitility.await().until(() -> bout.size() > 0);
					System.out.println( new String( bout.toByteArray()));
				}
			} finally {
				client.stop();
			}
		}
	}

	public static SshClient setupTestClient() {
		SshClient client = SshClient.setUpDefaultClient();
		client.setServerKeyVerifier(new ServerKeyVerifier() {

			@Override
			public boolean verifyServerKey(ClientSession clientSession, SocketAddress remoteAddress,
					PublicKey serverKey) {
				return true;
			}
		});
		client.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);
		client.setKeyIdentityProvider(KeyIdentityProvider.EMPTY_KEYS_PROVIDER);
		return client;
	}

}
