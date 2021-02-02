package biz.aQute.broker.api;

import java.util.function.Consumer;

import org.osgi.dto.DTO;

/**
 * A configured topic to publish or subscribe to.
 *
 */
public interface Topic {
	/**
	 * Subscribe to this topic
	 * 
	 * @param <T>
	 *            the type the caller expects to receive
	 * @param receiver
	 *            callback, must be quick
	 * @param type
	 *            the type class the caller expects to receive
	 * @return a closable to close this subscription
	 */
	<T> AutoCloseable subscribe(Consumer<T> receiver, Class<T> type);

	/**
	 * Publish to this topic.
	 * 
	 * @param data
	 *            the data to publish
	 * @return an error or a receipt.
	 */
	Receipt publish(DTO data);
}
