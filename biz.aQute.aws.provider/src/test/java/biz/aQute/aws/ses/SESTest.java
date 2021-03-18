package biz.aQute.aws.ses;

import biz.aQute.aws.credentials.UserCredentials;
import junit.framework.TestCase;

public class SESTest extends TestCase {
	UserCredentials	uc	= new UserCredentials();

	public void test() throws Exception {
		SESImpl ses = new SESImpl(uc.getAWSAccessKeyId(), uc.getAWSSecretKey(), uc.getProperty("awsregion","https://email.us-east-1.amazonaws.com/"), "ses@aQute.biz");
		String from = ses.subject("Hello Peter").to("ses@aqute.biz").text("Hello peter").from("ses@aqute.biz").send();
		System.out.println(from);
		assertNotNull(from);
	}

}
