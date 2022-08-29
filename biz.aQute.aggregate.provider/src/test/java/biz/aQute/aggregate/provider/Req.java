package biz.aQute.aggregate.provider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import biz.aQute.aggregate.api.Aggregate;

@Component(service = Req.class)
public class Req {
	@Reference
	IF[] list;


	@Aggregate(IF.class)
	interface AgIF {
		
	}

	interface AgIF1 extends Iterable<IF>{
		
	}
}
