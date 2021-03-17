package biz.aQute.aws.credentials;

import java.io.*;
import java.util.*;

public class UserCredentials {
	File		home	= new File((String) System.getProperties().get("user.home"));
	Properties	properties;

	public String getAWSAccessKeyId() {
		return getProperty("awsid");
	}

	public String getAWSSecretKey() {
		return getProperty("awssecret");
	}

	private String getProperty(String string) {
		return getProperties().getProperty(string);
	}

	private synchronized Properties getProperties(){
		if (properties != null)
			return properties;

		try {
			properties = new Properties();
			File aws = new File(home, ".aws");
			File pf = new File(aws, "properties");
			if (!pf.isFile()) {

				throw new FileNotFoundException();
			}

			try(InputStream in = new FileInputStream(pf)) {
				properties.load(in);
			}
		}
		catch (Exception e) {
			properties = new Properties();
			properties.putAll(System.getenv());
		}
		return properties;
	}
}
