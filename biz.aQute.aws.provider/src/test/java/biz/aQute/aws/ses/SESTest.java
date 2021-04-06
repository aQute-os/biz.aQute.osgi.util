package biz.aQute.aws.ses;

import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

import biz.aQute.aws.credentials.UserCredentials;

public class SESTest {
	UserCredentials	uc	= new UserCredentials();

	@Test
	@Ignore
	public void test() throws Exception {
		SESImpl ses = new SESImpl(uc.getAWSAccessKeyId(), uc.getAWSSecretKey(), uc.getProperty("awsregion","https://email.us-east-1.amazonaws.com/"), "ses@aQute.biz");
		String from = ses.subject("Hello Peter").to("ses@aqute.biz").text("Hello peter").from("ses@aqute.biz").send();
		System.out.println(from);
		assertNotNull(from);
	}

}
