package biz.aQute.aggregate.provider;

import java.util.Collection;

import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * A gogo command to see the aggregates
 */
@GogoCommand(scope = "aQute", function = {
	"aggregates"
})
@Component(service = AggregateGogo.class)
public class AggregateGogo {

	@Reference
	AggregateState state;

	public Collection<TrackedService> aggregates() {
		return state.trackedServices.values();
	}
}
