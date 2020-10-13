package biz.aQute.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

public class FSMTest {

	enum State {
		A, B, C
	}

	enum Event {
		E1, E2, E3, E4, E5, E6
	}

	FSM<State, Event, Runnable> fsm = new FSM<State, Event, Runnable>(State.A) {
	};

	@Test
	public void testSimple() {

		fsm.add(State.A, Event.E1, State.B, () -> System.out.println("A-E1-B"));
		fsm.add(State.B, Event.E2, State.A, () -> System.out.println("B-E2-A"));
		fsm.add(State.A, Event.E3, State.B);

		// existing transition, existing action
		Optional<Runnable> action = fsm.event(Event.E1);
		assertTrue(action.isPresent());
		action.get().run();

		action = fsm.event(Event.E1);
		assertFalse(action.isPresent());

		action = fsm.event(Event.E2);
		assertTrue(action.isPresent());
		action.get().run();

	}

	@Test
	public void testExistingTransitionAndAction() {
		Runnable action = () -> System.out.println("A-E1-B");
		fsm.add(State.A, Event.E1, State.B, action);
		Optional<Runnable> action2 = fsm.event(Event.E1);
		assertTrue(action2.isPresent());
		assertEquals(action, action2.get());
		assertEquals(State.B, fsm.state());
	}

	@Test
	public void testNonExistantTransition() {
		fsm.add(State.A, Event.E1, State.B);
		Optional<Runnable> action2 = fsm.event(Event.E2);
		assertFalse(action2.isPresent());
		assertEquals(State.A, fsm.state());
	}

	@Test
	public void testExistingTransitionAndNoAction() {
		fsm.add(State.A, Event.E1, State.B);
		Optional<Runnable> action2 = fsm.event(Event.E1);
		assertFalse(action2.isPresent());
		assertEquals(State.B, fsm.state());
	}

	@Test
	public void testHighestOrdinals() {
		fsm.add(State.A, Event.E1, State.C);
		fsm.add(State.C, Event.E6, State.B);
		fsm.event(Event.E1);
		assertEquals(State.C, fsm.state());
		fsm.event(Event.E6);
		assertEquals(State.B, fsm.state());
	}

	@Test
	public void testAll() {

		for (State s : State.values()) {
			for (Event e : Event.values()) {
				fsm.add(s, e, State.C);
			}
		}

		for (Event e : Event.values()) {
			for (State s : State.values()) {
				fsm.state(s);
				fsm.event(e);
				assertEquals(State.C, fsm.state());
			}
		}
	}
}
