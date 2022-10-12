package biz.aQute.aggregate.api;

import java.util.List;

public interface Aggregate<S> {
	
	List<S> getServices();

}
