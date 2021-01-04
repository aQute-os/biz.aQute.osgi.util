package biz.aQute.shell.sshd.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.junit.Test;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.launchpad.Service;
import aQute.lib.converter.Converter;
import biz.aQute.authentication.api.Authenticator;
import biz.aQute.shell.sshd.config.SshdConfig;

public class GogoSshdTest {

	final static LaunchpadBuilder	builder	= new LaunchpadBuilder().bndrun("tester.bndrun");

	@Service
	CommandProcessor				cp;

	@Test
	public void testAuthentication() throws Exception {
		try (Launchpad lp = builder.bundles("org.apache.felix.gogo.runtime").create().inject(this)) {
			SshdConfig config = Converter.cnv(SshdConfig.class, Collections.emptyMap());
			AtomicBoolean		authenticated= new AtomicBoolean();
			
			Authenticator auth = new Authenticator() {

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
			lp.register(Authenticator.class, auth);
			
			GogoSshd g = new GogoSshd(lp.getBundleContext(), cp, config);
			login();
			g.deactivate();
			assertThat(authenticated.get()).isTrue();
			assertThat(g.userid).isEqualTo("foo");
		}
	}
	
	
	private void login() throws Exception {
	    try (SshClient client = setupTestClient()) {
	        client.start();
	        client.setKeyIdentityProvider(null);
	        try (final ClientSession session = client.connect("user", "localhost", 8061).verify(7L, TimeUnit.SECONDS).getSession()) {
	            session.addPasswordIdentity("foo");
	            session.auth().verify(11L, TimeUnit.SECONDS);

	            ByteArrayOutputStream bout = new ByteArrayOutputStream();
	            
	            try (final ClientChannel channel = session.createShellChannel()) {
	            	
	                channel.setOut(bout);
	                channel.setErr(bout);
	                
	                channel.open().verify(15L, TimeUnit.SECONDS);
	                
	                OutputStream in = channel.getInvertedIn();
	                in.write("exit\r".getBytes());
	                in.flush();
	            }
	        } finally {
	            client.stop();
	        }
	    }
	}
	

	public static SshClient setupTestClient() {
		SshClient client = SshClient.setUpDefaultClient();
		client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
		client.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);
		client.setKeyIdentityProvider(KeyIdentityProvider.EMPTY_KEYS_PROVIDER);
		return client;
	}

}
