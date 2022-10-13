package biz.aQute.aggregate.test;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.aQute.aggregate.api.Aggregate;
import biz.aQute.osgi.service.util.ObjectClass;

@Component
public class Server {
	Logger logger = LoggerFactory.getLogger(Server.class);

	@Activate
	public Server() {
		logger.info("activated {}", guard.getServices());
	}

	@Deactivate
	public void deactivate() {
		logger.info("deactivated {}", guard.getServices());
	}

	@ObjectClass
	interface AgFoo extends Aggregate<Foo> {
	}

	@Reference
	AgFoo guard;
}
