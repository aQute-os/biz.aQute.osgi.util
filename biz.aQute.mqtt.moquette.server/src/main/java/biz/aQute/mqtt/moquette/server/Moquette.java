package biz.aQute.mqtt.moquette.server;


import java.io.IOException;
import java.util.Properties;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import biz.aQute.mqtt.moquette.server.config.MoquetteConfig;
import io.moquette.BrokerConstants;
import io.moquette.broker.Server;


@Designate(ocd=MoquetteConfig.class, factory=false)
@Component(immediate=true, service=Moquette.class, configurationPolicy = ConfigurationPolicy.REQUIRE, name=MoquetteConfig.PID)
public class Moquette {
	final Server mqttBroker = new Server();

	@Activate
	public Moquette(MoquetteConfig c) throws IOException {
		Properties p = new Properties();
		p.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, ""+c.allow_anonymous());
		p.setProperty(BrokerConstants.HOST_PROPERTY_NAME, c.host());
		p.setProperty(BrokerConstants.PORT_PROPERTY_NAME, Integer.toString( c.port()));
		
	    mqttBroker.startServer(p);
	}
	
	@Deactivate
	void deactivate() {
		mqttBroker.stopServer();
	}
}
