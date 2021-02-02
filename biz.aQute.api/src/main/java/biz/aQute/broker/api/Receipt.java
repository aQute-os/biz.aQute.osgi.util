package biz.aQute.broker.api;

import java.util.Optional;

public interface Receipt {
	Optional<String> getMessageId();
	QoS qos();
	Optional<String> sync(long timeoutMs);
}
