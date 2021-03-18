package biz.aQute.aws.ses;

import java.io.Closeable;
import java.security.NoSuchAlgorithmException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;

import biz.aQute.aws.config.SESConfig;
import biz.aQute.aws.impl.AWSImpl;
import biz.aQute.aws.impl.Protocol;
import biz.aQute.aws.impl.Request;
import biz.aQute.aws.ses.api.SES;

@Designate(ocd = SESConfig.class, factory = true)
@Component(configurationPid="biz.aQute.aws.ses",configurationPolicy=ConfigurationPolicy.REQUIRE)
public class SESImpl implements Closeable, SES {
	final Protocol	aws;
	final String from;


	@Activate
	public SESImpl(SESConfig config) throws NoSuchAlgorithmException {
		this(config.id(), config._secret(), config.region(), config.from());
	}


	SESImpl(String awsAccessKeyId, String awsSecretKey, String region, String from) throws NoSuchAlgorithmException {
		AWSImpl aws = new AWSImpl(awsAccessKeyId, awsSecretKey, region);
		this.aws = new Protocol(aws,region, null, 3);
		this.from = from;
	}


	@Override
	public SESRequestImpl subject(String subject) throws Exception {
		Request request = aws.action("SendEmail");
		if (subject != null)
			request.arg("Message.Subject.Data", subject);
		return new SESRequestImpl(request);
	}

	public class SESRequestImpl implements SESRequest {
		final Request	request;
		int				to		= 1;
		int				cc		= 1;
		int				bcc		= 1;
		int				reply	= 1;
		boolean			from	= false;

		SESRequestImpl(Request request) {
			this.request = request;
		}

		@Override
		public SESRequestImpl html(String html) {
			request.arg("Message.Body.Html.Data", html);
			return this;

		}

		@Override
		public SESRequestImpl text(String text) {
			request.arg("Message.Body.Text.Data", text);
			return this;

		}

		@Override
		public SESRequestImpl to(String address) {
			request.arg("Destination.ToAddresses.member." + to++, address);
			return this;

		}

		@Override
		public SESRequestImpl cc(String address) {
			request.arg("Destination.CcAddresses.member." + cc++, address);
			return this;

		}

		@Override
		public SESRequestImpl returnPath(String address) {
			request.arg("ReturnPath", address);
			return this;

		}

		@Override
		public SESRequestImpl replyTo(String address) {
			request.arg("ReplyToAddresses.member." + reply++, address);
			return this;
		}

		@Override
		public SESRequestImpl bcc(String address) {
			request.arg("Destination.CcAddresses.member." + bcc++, address);
			return this;
		}

		@Override
		public SESRequestImpl from(String address) {
			from=true;
			request.arg("Source", address.toLowerCase());
			return this;
		}

		@Override
		public String send() throws Exception {
			if ( !from) {
				from(SESImpl.this.from);
				from=true;
			}
			return request.string("SendEmailResponse/SendEmailResult/MessageId");
		}
	}

	public void close() {

	}
}
