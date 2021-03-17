package biz.aQute.aws.ses;

import biz.aQute.aws.AWS;
import biz.aQute.aws.credentials.UserCredentials;
import junit.framework.TestCase;

public class SESTest extends TestCase {
	UserCredentials	uc	= new UserCredentials();

	public void test() throws Exception {
		AWS aws = new AWS(uc.getAWSAccessKeyId(), uc.getAWSSecretKey());
		SES ses = aws.ses();
		String from = ses.subject("Hello Peter").to("ses@aqute.biz").text("Hello peter").from("ses@aqute.biz").send();
		System.out.println(from);
		assertNotNull(from);
	}

}
