package biz.aQute.broker.api;

public interface Subscriber<T> {
	String	broker		= "broker";
	String	topics		= "topics";
	
	void receive( T data);
}
