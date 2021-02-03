package biz.aQute.cryptonicom;

import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import biz.aQute.pki.api.Cryptonicom;

public class CyrptonicomImpl implements Cryptonicom {

	@Override
	public Map<String, String> getAliases() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] sign(String alias, byte[] data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean verify(String alias, byte[] data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SSLSocketFactory getSocketFactory(String alias) {
		// TODO Auto-generated method stub
		return null;
	}

}
