package biz.aQute.broker.api;

import java.io.Closeable;

public interface Broker {
	<T> Topic<T> topic(String topic, boolean retain, int qos, Class<T> type);

	<T> Closeable subscribe(Subscriber<T> subscriber, Class<T> type, int qos, String... topic);
}
