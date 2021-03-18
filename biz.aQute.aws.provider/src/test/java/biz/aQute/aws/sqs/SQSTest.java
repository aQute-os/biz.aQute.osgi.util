package biz.aQute.aws.sqs;

import java.util.UUID;

import biz.aQute.aws.credentials.UserCredentials;
import biz.aQute.aws.impl.AWSImpl;
import biz.aQute.aws.impl.Protocol;
import junit.framework.TestCase;

public class SQSTest extends TestCase {
	UserCredentials	uc	= new UserCredentials();
	AWSImpl				aws;

	public void test() throws Exception {
		aws = new AWSImpl(uc.getAWSAccessKeyId(), uc.getAWSSecretKey(), "https://sqs.us-east-1.amazonaws.com");

		SQSImpl sqs = aws.sqs();

		MessageQueue q1 = sqs.getQueue("test");
		assertNotNull(q1);

	}

	/**
	 * Create/Delete a queue and in the middle try some send/receive scenarios.
	 *
	 * @throws Exception
	 */
	public void queueLifeCycle() throws Exception {
		aws = new AWSImpl(uc.getAWSAccessKeyId(), uc.getAWSSecretKey(), "https://sqs.us-east-1.amazonaws.com");
		Protocol protocol = new Protocol(aws, "https://sqs.us-east-1.amazonaws.com", SQSImpl.version, 2);

		SQSImpl sqs = new SQSImpl(protocol);

		String name = UUID.randomUUID().toString();
		MessageQueue q1 = sqs.getQueue(name);
		try {
			assertNull(q1);
			q1 = sqs.createQueue(name).visibilityTimeOut(1).get();
			assertNotNull(q1);
			assertEquals(name, q1.getName());
			MessageQueue q2 = sqs.createQueue(name).get();

			// Try basic send receive
			sendReceive(q1);

			// Check for expiration
			sendReceiveExpire(q1);

			assertEquals(q1, q1);

			// Multiple deletes
			sqs.deleteQueue(q1);
			sqs.deleteQueue(q2);
			try {
				q1.receive();
				fail("not supposed to get here");
			}
			catch (Exception e) {
				// Ignore
				// thrown because queue does not exist
			}
		}
		finally {
			// make sure
			if (q1 != null)
				sqs.deleteQueue(q1);
		}
		// Ensure queue does not exist
		assertNull(sqs.getQueue(name));
	}

	/**
	 * Send a message and try to receive it.
	 *
	 * @param q
	 * @throws Exception
	 */
	public void sendReceive(MessageQueue q) throws Exception {
		Message m1 = q.send("Hello World");
		assertNotNull(m1);
		Message m2 = q.receive();
		try {
			assertEquals(m1.getBody(), m2.getBody());
		}
		finally {
			m2.delete();
		}
		Thread.sleep(2000);
		m2 = q.receive();
		assertNull(m2);
	}

	public void sendReceiveExpire(MessageQueue q) throws Exception {
		Message m1 = q.send("Hello World");
		assertNotNull(m1);
		Message m2 = q.receive();
		assertEquals(m1.getBody(), m2.getBody());

		Thread.sleep(60000);
		Message m3 = q.receive();
		assertNotNull(m3);

		assertEquals(m1.getBody(), m3.getBody());
		m2.delete();
	}

}