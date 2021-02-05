package biz.aQute.broker.api;

public interface Subscriber<T> {
	void receive( T data);
}
