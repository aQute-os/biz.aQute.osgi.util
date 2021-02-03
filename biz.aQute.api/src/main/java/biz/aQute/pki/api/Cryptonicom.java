package biz.aQute.pki.api;

import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

public interface Cryptonicom {

	Map<String, String> getAliases();

	byte[] sign(String alias, byte[] data);

	boolean verify(String alias, byte[] data);

	SSLSocketFactory getSocketFactory(String alias);
}
