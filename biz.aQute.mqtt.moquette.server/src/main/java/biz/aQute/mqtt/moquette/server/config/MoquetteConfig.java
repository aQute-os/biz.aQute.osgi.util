package biz.aQute.mqtt.moquette.server.config;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/*
 * ##############################################
#  Moquette configuration file. 
#
#  The synthax is equals to mosquitto.conf
# 
##############################################

port 0

#websocket_port 8080

host localhost

#Password file
#password_file password_file.conf

#ssl_port 8883
#jks_path serverkeystore.jks
#key_store_password passw0rdsrv
#key_manager_password passw0rdsrv

allow_anonymous true
 */
@ObjectClassDefinition
public @interface MoquetteConfig {
	static final String PID = "biz.aQute.mqtt.moquette.server";

	String host();

	int port();

	int websocket_port() default 8081;

	String password_file() default "password_file.conf";

	int ssl_port();

	String jks_path();

	String key_store_password();

	String key_manager_password();

	boolean allow_anonymous() default false;

}
