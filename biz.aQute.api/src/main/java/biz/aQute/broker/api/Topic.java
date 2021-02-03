package biz.aQute.broker.api;

import org.osgi.dto.DTO;

/**
 * A configured topic to publish or subscribe to.
 *
 */
public interface Topic {
	String		topic="topic";
	String		broker="broker";
	
	/**
	 * Publish to this topic.
	 * 
	 * @param data
	 *            the data to publish
	 */
	void publish(DTO data);
}
