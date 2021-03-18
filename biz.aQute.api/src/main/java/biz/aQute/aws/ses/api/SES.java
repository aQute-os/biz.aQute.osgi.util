package biz.aQute.aws.ses.api;

public interface SES {

	SESRequest subject(String subject) throws Exception;


	interface SESRequest {

		SESRequest html(String html);

		SESRequest text(String text);

		SESRequest to(String address);


		SESRequest from(String address);

		SESRequest bcc(String address);

		SESRequest replyTo(String address);

		SESRequest cc(String address);

		SESRequest returnPath(String address);

		String send() throws Exception;
	}
}
