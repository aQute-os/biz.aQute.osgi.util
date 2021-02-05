package biz.aQute.broker.api;

/**
 * A configured topic to publish or subscribe to.
 *
 */
public interface Topic<T> {
	
	/**
	 * Publish to this topic.
	 * 
	 * @param data
	 *            the data to publish
	 * @throws Exception 
	 */
	void publish(T data) throws Exception;
}
