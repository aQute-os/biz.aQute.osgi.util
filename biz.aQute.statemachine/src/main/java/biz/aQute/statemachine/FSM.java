package biz.aQute.statemachine;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Classic Moore efficient finite state machine.
 * <p>
 * A Moore state machine has no inputs. Any memory is therefore encoded in the
 * states. This state machine had a table of transitions defined by the user.
 * Once the table is setup the event method can be setup to call events.
 * Transitions define a next state, this next state is set atomically. If an
 * action is defined, this is returned from the event method. The state transitions
 * should be defined in such a way that they can be used without requiring
 * the lock of the state machine.
 *
 */
public class FSM<State extends Enum<?>, Event extends Enum<?>, Action> {

	static class Transition<State extends Enum<?>, Action> {
		Transition(State next, Action action) {
			this.nextState = next;
			this.action = action;
		}

		final State		nextState;
		final Action	action;
	}

	final Transition<State, Action>[]	transitions;
	final int							nrOfEvents;

	private State						state;

	/**
	 * Create an FSM by specifying the state and even enum classes explicitly
	 * 
	 * @param sclass
	 *            the Enum class used for the states
	 * @param eclass
	 *            the Enum class used for the events
	 * @param initial
	 *            the initial state
	 */
	@SuppressWarnings("unchecked")
	public FSM(Class<State> sclass, Class<Event> eclass, State initial) {
		assert sclass.isEnum();
		assert eclass.isEnum();

		this.state = initial;
		this.nrOfEvents = eclass.getEnumConstants().length;
		int states = sclass.getEnumConstants().length;
		this.transitions = new Transition[states * nrOfEvents];
	}

	/**
	 * Create an FSM by creating an inner class. The super class will then hold
	 * the event and state class as type parameters.
	 * 
	 * @param initial
	 *            the initial state
	 */
	@SuppressWarnings("unchecked")
	public FSM(State initial) {

		// get the state and event types from our super class declaration
		ParameterizedType superClassGenericType = (ParameterizedType) getClass().getGenericSuperclass();
		Type[] types = superClassGenericType.getActualTypeArguments();
		Class<? extends Enum<?>> eclass = (Class<? extends Enum<?>>) types[1];
		Class<? extends Enum<?>> sclass = (Class<? extends Enum<?>>) types[0];

		this.state = initial;
		this.nrOfEvents = eclass.getEnumConstants().length;
		int states = sclass.getEnumConstants().length;
		this.transitions = new Transition[states * nrOfEvents];
	}

	/**
	 * Add a new transition with an action. The action will be returned when
	 * event is called.
	 * 
	 * @param state
	 *            the from state
	 * @param event
	 *            the event
	 * @param next
	 *            the next state
	 * @param action
	 *            the action
	 * @return this
	 */
	public FSM<State, Event, Action> add(State state, Event event, State next, Action action) {
		int index = state.ordinal() * nrOfEvents + event.ordinal();

		assert index < transitions.length;
		assert transitions[index] == null;

		transitions[index] = new Transition<>(next, action);

		return this;
	}

	/**
	 * Add a new transition without an action.
	 * 
	 * @param from
	 *            the from state
	 * @param event
	 *            the event that happened
	 * @param to
	 *            the state to move to
	 * @return this
	 */
	public FSM<State, Event, Action> add(State from, Event event, State to) {
		return this.add(from, event, to, null);
	}

	/**
	 * Send an event to the state machine. If there is a defined transition for
	 * the current <state,event> pair then the transition is executed. The
	 * return of this method is the defined parameter when the transition was
	 * added.
	 * 
	 * @param event
	 *            The event that happened
	 * @return the optional action
	 */
	public Optional<Action> event(Event event) {
		synchronized (transitions) {
			int index = state.ordinal() * nrOfEvents + event.ordinal();
			Transition<State, Action> t = transitions[index];
			if (t == null)
				return Optional.empty();

			state = t.nextState;
			return Optional.ofNullable(t.action);
		}
	}

	/**
	 * Return the current state. The method does synchronize so there is a
	 * proper before/after relation with this call. I.e. all events that
	 * happened before you call state should have been processed. Clearly the
	 * return value represents a snapshot, when inspecting the state machine
	 * could already have moved on.
	 * 
	 * @return snapshot of the current state
	 */
	public State state() {
		synchronized (transitions) { // ensure before/after
			return state;
		}
	}

	/**
	 * Set a new state. This clearly bypasses the transitions planned and should
	 * therefore only be used in testing or very extraordinary cases.
	 * 
	 * @param state
	 *            the new state
	 * @return the previous state
	 */
	public State state(State state) {
		synchronized (transitions) { // ensure before/after
			State old = this.state;
			this.state = state;
			return old;
		}
	}
}
