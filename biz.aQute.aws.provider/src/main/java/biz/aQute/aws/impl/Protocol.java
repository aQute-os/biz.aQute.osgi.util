package biz.aQute.aws.impl;

public class Protocol {
	final int		signature;
	final AWSImpl		aws;
	final String	endpoint;
	final String	version;

	public Protocol(AWSImpl aws, String endpoint, String version, int signature) {
		this.aws = aws;
		this.endpoint = endpoint;
		this.version = version;
		this.signature = signature;
	}

	public Request action(String action) throws Exception {
		return aws.request(this, action);
	}
}
