package biz.aQute.aws.ses.api;

import org.osgi.annotation.versioning.ProviderType;

/**
 * An interface to the Simple Email Service from AWS.
 * <p>
 * This interface allows you to send emails.
 */
@ProviderType
public interface SES {

	/**
	 * Create a new email.
	 *
	 * @param subject
	 *            the subject of the mail
	 * @return the email request builder
	 */
	SESRequest subject(String subject) throws Exception;

	/**
	 * The SES Request builder.
	 *
	 */
	interface SESRequest {

		/**
		 * Provide HTML content
		 *
		 * @param html
		 *            the html content
		 */
		SESRequest html(String html);

		/**
		 * Provide plain text content
		 *
		 * @param html
		 *            the html content
		 */
		SESRequest text(String text);

		/**
		 * The destination
		 *
		 * @param html
		 *            the html content
		 */
		SESRequest to(String address);

		/**
		 * The source
		 *
		 * @param address
		 */

		/**
		 * The source
		 *
		 * @param address
		 */
		SESRequest from(String address);

		/**
		 * The blank carbon copy
		 *
		 * @param address
		 */
		SESRequest bcc(String address);

		/**
		 * Reply to
		 *
		 * @param address
		 */
		SESRequest replyTo(String address);

		/**
		 * Carbon Copy
		 *
		 * @param address
		 */
		SESRequest cc(String address);

		/**
		 * Return Path
		 *
		 * @param address
		 */
		SESRequest returnPath(String address);

		/**
		 * Send the email
		 *
		 * @return the email id
		 */
		String send() throws Exception;
	}
}
