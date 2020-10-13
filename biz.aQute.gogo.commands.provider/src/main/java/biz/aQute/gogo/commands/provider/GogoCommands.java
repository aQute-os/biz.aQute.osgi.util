package biz.aQute.gogo.commands.provider;

import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogService;

@GogoCommand(scope = "aqute", function = { "lentry" })
@Component(service=GogoCommands.class)
public class GogoCommands {

	@Reference LogService log;

	@SuppressWarnings("deprecation")
	public void lentry( LogLevel level, String message) {
		log.log(level.ordinal(), message);
	}
}
