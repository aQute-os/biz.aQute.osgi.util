package biz.aQute.shell.sshd.provider;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component
public class GogoConsole {

	private CommandSessionHandler handler;

	@Activate
	public GogoConsole(@Reference CommandProcessor cp, BundleContext context) throws Exception {
		String goshArgs = context.getProperty("gosh.args");
		if (goshArgs!=null && goshArgs.contains("--nointeractive"))
			return;

		Map<String, String> env = new HashMap<>();
		env.put("TERM", "plain");
		FileInputStream in = new FileInputStream(FileDescriptor.in);
		FileOutputStream out = new FileOutputStream(FileDescriptor.out);
		FileOutputStream err = new FileOutputStream(FileDescriptor.err);

		handler = new CommandSessionHandler(context, "console", env, in, out,
				err, cp, (e, s) -> {
				});

		handler.getANSI().setNoCR();
		handler.getANSI().setNoEcho();
	}

	@Deactivate
	void deactivate() throws IOException {
		if (handler != null)
			handler.close();
	}
}
