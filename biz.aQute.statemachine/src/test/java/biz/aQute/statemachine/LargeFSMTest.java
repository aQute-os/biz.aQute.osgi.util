package biz.aQute.statemachine;

import static biz.aQute.statemachine.LargeFSMTest.Action.BEGIN;
import static biz.aQute.statemachine.LargeFSMTest.Action.END;
import static biz.aQute.statemachine.LargeFSMTest.Action.ERROR;
import static biz.aQute.statemachine.LargeFSMTest.Action.NONE;
import static biz.aQute.statemachine.LargeFSMTest.Action.RESTART;
import static biz.aQute.statemachine.LargeFSMTest.Event.ALLOW;
import static biz.aQute.statemachine.LargeFSMTest.Event.DISABLE;
import static biz.aQute.statemachine.LargeFSMTest.Event.ENABLE;
import static biz.aQute.statemachine.LargeFSMTest.Event.FINISH;
import static biz.aQute.statemachine.LargeFSMTest.Event.FORBID;
import static biz.aQute.statemachine.LargeFSMTest.Event.FORCE;
import static biz.aQute.statemachine.LargeFSMTest.Event.UPDATE;
import static biz.aQute.statemachine.LargeFSMTest.State.DSBL_ALL;
import static biz.aQute.statemachine.LargeFSMTest.State.DSBL_FRB;
import static biz.aQute.statemachine.LargeFSMTest.State.IDLE_ALL;
import static biz.aQute.statemachine.LargeFSMTest.State.IDLE_FRB;
import static biz.aQute.statemachine.LargeFSMTest.State.JOIN_ALL;
import static biz.aQute.statemachine.LargeFSMTest.State.JOIN_FRB;
import static biz.aQute.statemachine.LargeFSMTest.State.RSTR_ALL;
import static biz.aQute.statemachine.LargeFSMTest.State.RSTR_ALL_HGH;
import static biz.aQute.statemachine.LargeFSMTest.State.RSTR_FRB;
import static biz.aQute.statemachine.LargeFSMTest.State.RSTR_FRB_HGH;
import static biz.aQute.statemachine.LargeFSMTest.State.SCHD_FRB;
import static biz.aQute.statemachine.LargeFSMTest.State.SCHD_FRB_HGH;
import static biz.aQute.statemachine.LargeFSMTest.State.UPDT_ALL;
import static biz.aQute.statemachine.LargeFSMTest.State.UPDT_FRB;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LargeFSMTest {

	enum Event {
		UPDATE, // start a low priority update
		FORCE, // start a high priority update
		FINISH, // update (low or high) finished
		DISABLE, // disable processing (waits for a current update to finish)
		ENABLE, // enable processing
		ALLOW, // allow updating (balanced with FORBID)
		FORBID // forbid updating (balanced with ALLOW)
	}

	enum State {
		// ALLOQ FORBID ALLOW HIGH FORBID HIGH
		DSBL_ALL, DSBL_FRB, // disable
		IDLE_ALL, IDLE_FRB, // idle
		SCHD_FRB, SCHD_FRB_HGH, // schedule update when allowed
		UPDT_ALL, UPDT_FRB, // currently updating
		RSTR_ALL, RSTR_FRB, RSTR_ALL_HGH, RSTR_FRB_HGH, // restart update after
														// finishing
		JOIN_ALL, JOIN_FRB // disable after update is finished
	}

	enum Action {
		BEGIN, // begin update
		END, // end update
		NONE, // intermediate
		RESTART, // restart update
		ERROR // invalid transition
	}

	public FSM<State, Event, Action> largeTest() {
		FSM<State, Event, Action> fsm = new FSM<State, Event, Action>(State.DSBL_FRB) {
		};
		fsm.add(DSBL_ALL, UPDATE, DSBL_ALL, NONE);
		fsm.add(DSBL_ALL, FORCE, DSBL_ALL, NONE);
		fsm.add(DSBL_ALL, FINISH, DSBL_ALL, END);
		fsm.add(DSBL_ALL, DISABLE, DSBL_ALL, NONE);
		fsm.add(DSBL_ALL, ENABLE, IDLE_ALL, NONE);
		fsm.add(DSBL_ALL, ALLOW, DSBL_ALL, ERROR);
		fsm.add(DSBL_ALL, FORBID, DSBL_FRB, NONE);

		fsm.add(DSBL_FRB, UPDATE, DSBL_FRB, NONE);
		fsm.add(DSBL_FRB, FORCE, DSBL_FRB, NONE);
		fsm.add(DSBL_FRB, FINISH, DSBL_FRB, END);
		fsm.add(DSBL_FRB, DISABLE, DSBL_FRB, NONE);
		fsm.add(DSBL_FRB, ENABLE, IDLE_FRB, NONE);
		fsm.add(DSBL_FRB, ALLOW, DSBL_ALL, NONE);
		fsm.add(DSBL_FRB, FORBID, DSBL_FRB, ERROR);

		fsm.add(IDLE_ALL, UPDATE, UPDT_ALL, BEGIN);
		fsm.add(IDLE_ALL, FORCE, UPDT_ALL, BEGIN);
		fsm.add(IDLE_ALL, FINISH, IDLE_ALL, END);
		fsm.add(IDLE_ALL, DISABLE, DSBL_ALL, NONE);
		fsm.add(IDLE_ALL, ENABLE, IDLE_ALL, NONE);
		fsm.add(IDLE_ALL, ALLOW, IDLE_ALL, ERROR);
		fsm.add(IDLE_ALL, FORBID, IDLE_FRB, NONE);

		fsm.add(IDLE_FRB, UPDATE, SCHD_FRB, NONE);
		fsm.add(IDLE_FRB, FORCE, UPDT_FRB, BEGIN);
		fsm.add(IDLE_FRB, FINISH, IDLE_FRB, END);
		fsm.add(IDLE_FRB, DISABLE, DSBL_FRB, NONE);
		fsm.add(IDLE_FRB, ENABLE, IDLE_FRB, NONE);
		fsm.add(IDLE_FRB, ALLOW, IDLE_ALL, NONE);
		fsm.add(IDLE_FRB, FORBID, IDLE_FRB, ERROR);

		fsm.add(SCHD_FRB, UPDATE, SCHD_FRB, NONE);
		fsm.add(SCHD_FRB, FORCE, SCHD_FRB_HGH, NONE);
		fsm.add(SCHD_FRB, FINISH, UPDT_FRB, RESTART);
		fsm.add(SCHD_FRB, DISABLE, JOIN_FRB, NONE);
		fsm.add(SCHD_FRB, ENABLE, SCHD_FRB, NONE);
		fsm.add(SCHD_FRB, ALLOW, UPDT_ALL, NONE);
		fsm.add(SCHD_FRB, FORBID, SCHD_FRB, ERROR);

		fsm.add(SCHD_FRB_HGH, UPDATE, SCHD_FRB_HGH, NONE);
		fsm.add(SCHD_FRB_HGH, FORCE, SCHD_FRB_HGH, NONE);
		fsm.add(SCHD_FRB_HGH, FINISH, UPDT_FRB, RESTART);
		fsm.add(SCHD_FRB_HGH, DISABLE, JOIN_FRB, NONE);
		fsm.add(SCHD_FRB_HGH, ENABLE, SCHD_FRB_HGH, NONE);
		fsm.add(SCHD_FRB_HGH, ALLOW, UPDT_ALL, NONE);
		fsm.add(SCHD_FRB_HGH, FORBID, SCHD_FRB_HGH, ERROR);

		fsm.add(UPDT_ALL, UPDATE, RSTR_ALL, NONE);
		fsm.add(UPDT_ALL, FORCE, RSTR_ALL_HGH, NONE);
		fsm.add(UPDT_ALL, FINISH, IDLE_ALL, END);
		fsm.add(UPDT_ALL, DISABLE, JOIN_ALL, NONE);
		fsm.add(UPDT_ALL, ENABLE, UPDT_ALL, NONE);
		fsm.add(UPDT_ALL, ALLOW, UPDT_ALL, ERROR);
		fsm.add(UPDT_ALL, FORBID, UPDT_FRB, NONE);

		fsm.add(UPDT_FRB, UPDATE, RSTR_FRB, NONE);
		fsm.add(UPDT_FRB, FORCE, RSTR_FRB_HGH, NONE);
		fsm.add(UPDT_FRB, FINISH, IDLE_FRB, END);
		fsm.add(UPDT_FRB, DISABLE, JOIN_FRB, NONE);
		fsm.add(UPDT_FRB, ENABLE, UPDT_FRB, NONE);
		fsm.add(UPDT_FRB, ALLOW, UPDT_ALL, NONE);
		fsm.add(UPDT_FRB, FORBID, UPDT_FRB, ERROR);

		fsm.add(RSTR_ALL, UPDATE, RSTR_ALL, NONE);
		fsm.add(RSTR_ALL, FORCE, RSTR_ALL_HGH, NONE);
		fsm.add(RSTR_ALL, FINISH, UPDT_ALL, RESTART);
		fsm.add(RSTR_ALL, DISABLE, JOIN_ALL, NONE);
		fsm.add(RSTR_ALL, ENABLE, RSTR_ALL, NONE);
		fsm.add(RSTR_ALL, ALLOW, RSTR_ALL, ERROR);
		fsm.add(RSTR_ALL, FORBID, RSTR_FRB, NONE);

		fsm.add(RSTR_FRB, UPDATE, RSTR_FRB, NONE);
		fsm.add(RSTR_FRB, FORCE, RSTR_FRB_HGH, NONE);
		fsm.add(RSTR_FRB, FINISH, UPDT_FRB, RESTART);
		fsm.add(RSTR_FRB, DISABLE, JOIN_FRB, NONE);
		fsm.add(RSTR_FRB, ENABLE, RSTR_FRB, NONE);
		fsm.add(RSTR_FRB, ALLOW, RSTR_ALL, NONE);
		fsm.add(RSTR_FRB, FORBID, RSTR_FRB, ERROR);

		fsm.add(RSTR_ALL_HGH, UPDATE, RSTR_ALL_HGH, NONE);
		fsm.add(RSTR_ALL_HGH, FORCE, RSTR_ALL_HGH, NONE);
		fsm.add(RSTR_ALL_HGH, FINISH, UPDT_ALL, RESTART);
		fsm.add(RSTR_ALL_HGH, DISABLE, JOIN_ALL, NONE);
		fsm.add(RSTR_ALL_HGH, ENABLE, RSTR_ALL_HGH, NONE);
		fsm.add(RSTR_ALL_HGH, ALLOW, RSTR_ALL_HGH, ERROR);
		fsm.add(RSTR_ALL_HGH, FORBID, RSTR_FRB_HGH, NONE);

		fsm.add(RSTR_FRB_HGH, UPDATE, RSTR_FRB_HGH, NONE);
		fsm.add(RSTR_FRB_HGH, FORCE, RSTR_FRB_HGH, NONE);
		fsm.add(RSTR_FRB_HGH, FINISH, UPDT_FRB, RESTART);
		fsm.add(RSTR_FRB_HGH, DISABLE, JOIN_FRB, NONE);
		fsm.add(RSTR_FRB_HGH, ENABLE, RSTR_FRB_HGH, NONE);
		fsm.add(RSTR_FRB_HGH, ALLOW, RSTR_ALL_HGH, NONE);
		fsm.add(RSTR_FRB_HGH, FORBID, RSTR_FRB_HGH, ERROR);

		fsm.add(JOIN_ALL, UPDATE, JOIN_ALL, NONE);
		fsm.add(JOIN_ALL, FORCE, JOIN_ALL, NONE);
		fsm.add(JOIN_ALL, FINISH, DSBL_ALL, END);
		fsm.add(JOIN_ALL, DISABLE, JOIN_ALL, NONE);
		fsm.add(JOIN_ALL, ENABLE, UPDT_ALL, NONE);
		fsm.add(JOIN_ALL, ALLOW, JOIN_ALL, ERROR);
		fsm.add(JOIN_ALL, FORBID, JOIN_FRB, NONE);

		fsm.add(JOIN_FRB, UPDATE, JOIN_FRB, NONE);
		fsm.add(JOIN_FRB, FORCE, JOIN_FRB, NONE);
		fsm.add(JOIN_FRB, FINISH, DSBL_FRB, END);
		fsm.add(JOIN_FRB, DISABLE, JOIN_FRB, NONE);
		fsm.add(JOIN_FRB, ENABLE, UPDT_FRB, NONE);
		fsm.add(JOIN_FRB, ALLOW, JOIN_ALL, NONE);
		fsm.add(JOIN_FRB, FORBID, JOIN_FRB, ERROR);
		return fsm;
	}

	/**
	 * <pre>
	┌──────────┬─────────────┬────────┬──────────┬───────┬───────┐
	│this/Trace│state        │event   │transition│process│allowed│
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace⁰    │DSBL_FRB⁰    │ALLOW⁰  │NONE⁰     │false⁰ │false⁰ │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace¹    │DSBL_ALL⁰    │ENABLE⁰ │NONE⁰     │false⁰ │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace²    │IDLE_ALL⁰    │FORCE⁰  │BEGIN⁰    │false⁰ │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace³    │UPDT_ALL⁰    │DISABLE⁰│NONE⁰     │true⁰  │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace⁴    │JOIN_ALL⁰    │FINISH⁰ │END⁰      │true⁰  │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace⁵    │DSBL_ALL⁰    │ENABLE⁰ │NONE⁰     │false⁰ │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace⁶    │IDLE_ALL⁰    │FORBID⁰ │NONE⁰     │false⁰ │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace⁷    │IDLE_FRB⁰    │FORCE⁰  │BEGIN⁰    │false⁰ │false⁰ │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace⁸    │UPDT_FRB⁰    │ALLOW⁰  │NONE⁰     │true⁰  │false⁰ │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace⁹    │UPDT_ALL⁰    │FINISH⁰ │END⁰      │true⁰  │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace¹⁰   │IDLE_ALL⁰    │FORCE⁰  │BEGIN⁰    │false⁰ │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace¹¹   │UPDT_ALL⁰    │FINISH⁰ │END⁰      │true⁰  │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace¹²   │IDLE_ALL⁰    │FORCE⁰  │BEGIN⁰    │false⁰ │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace¹³   │UPDT_ALL⁰    │FORBID⁰ │NONE⁰     │true⁰  │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace¹⁴   │UPDT_FRB⁰    │ALLOW⁰  │NONE⁰     │true⁰  │false⁰ │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace¹⁵   │UPDT_ALL⁰    │FINISH⁰ │END⁰      │true⁰  │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace¹⁶   │IDLE_ALL⁰    │FORBID⁰ │NONE⁰     │false⁰ │true⁰  │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace¹⁷   │IDLE_FRB⁰    │FORCE⁰  │BEGIN⁰    │false⁰ │false⁰ │
	├──────────┼─────────────┼────────┼──────────┼───────┼───────┤
	│Trace¹⁸   │UPDT_FRB⁰    │FORCE⁰  │NONE⁰     │true⁰  │false⁰ │
	└──────────┴─────────────┴────────┴──────────┴───────┴───────┘
	 * </pre>
	 */

	@Test
	public void testTrace() {
		FSM<State, Event, Action> fsm = largeTest();
		assertEquals(DSBL_FRB, fsm.state());
		assertEquals(NONE, fsm.event(ALLOW).get());

		assertEquals(DSBL_ALL, fsm.state());
		assertEquals(NONE, fsm.event(ENABLE).get());

		assertEquals(IDLE_ALL, fsm.state());
		assertEquals(BEGIN, fsm.event(FORCE).get());

		assertEquals(UPDT_ALL, fsm.state());
		assertEquals(NONE, fsm.event(DISABLE).get());

		assertEquals(JOIN_ALL, fsm.state());
		assertEquals(END, fsm.event(FINISH).get());

		assertEquals(DSBL_ALL, fsm.state());
		assertEquals(END, fsm.event(FINISH).get());

		assertEquals(DSBL_ALL, fsm.state());
		assertEquals(NONE, fsm.event(ENABLE).get());

		assertEquals(IDLE_ALL, fsm.state());
		assertEquals(NONE, fsm.event(FORBID).get());

		assertEquals(IDLE_FRB, fsm.state());
		assertEquals(BEGIN, fsm.event(FORCE).get());

		assertEquals(UPDT_FRB, fsm.state());
		assertEquals(NONE, fsm.event(ALLOW).get());

		assertEquals(UPDT_ALL, fsm.state());
		assertEquals(END, fsm.event(FINISH).get());

		assertEquals(IDLE_ALL, fsm.state());
		assertEquals(BEGIN, fsm.event(FORCE).get());

		assertEquals(UPDT_ALL, fsm.state());
		assertEquals(END, fsm.event(FINISH).get());

		assertEquals(IDLE_ALL, fsm.state());
		assertEquals(BEGIN, fsm.event(FORCE).get());

		assertEquals(UPDT_ALL, fsm.state());
		assertEquals(NONE, fsm.event(FORBID).get());

		assertEquals(UPDT_FRB, fsm.state());
		assertEquals(NONE, fsm.event(ALLOW).get());

		assertEquals(UPDT_ALL, fsm.state());
		assertEquals(END, fsm.event(FINISH).get());

		assertEquals(IDLE_ALL, fsm.state());
		assertEquals(NONE, fsm.event(FORBID).get());

		assertEquals(IDLE_FRB, fsm.state());
		assertEquals(BEGIN, fsm.event(FORCE).get());

		assertEquals(UPDT_FRB, fsm.state());
		assertEquals(NONE, fsm.event(FORCE).get());

	}

}
