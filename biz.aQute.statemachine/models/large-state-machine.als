
open util/ordering[Trace]

enum Event {
    	UPDATE,	-- start a low priority update
    	FORCE,	-- start a high priority update
    	FINISH,	-- update (low or high) finished
    	DISABLE,-- disable processing (waits for a current update to finish)
    	ENABLE,	-- enable processing
    	ALLOW,	-- allow updating (balanced with FORBID)
    	FORBID	-- forbid updating (balanced with ALLOW)
}

enum State {
	DSBL_ALL,	DSBL_FRB,					-- disable
	IDLE_ALL,	IDLE_FRB,					-- idle
			    SCHD_FRB,   SCHD_FRB_HGH,	-- schedule update when allowed
	UPDT_ALL,	UPDT_FRB,					-- currently updating
	RSTR_ALL,	RSTR_FRB,	RSTR_ALL_HGH,	RSTR_FRB_HGH,	-- restart update after finishing
	JOIN_ALL,	JOIN_FRB		                                 -- disable after update is finished
}

enum Action {
	BEGIN,	-- begin update
	END,   	-- end update
	NONE,	-- intermediate
	RESTART,-- restart update
	ERROR	-- invalid transition
}

fun states : State -> Event -> State -> Action { 

	DSBL_ALL	->	UPDATE	-> DSBL_ALL	-> NONE
+	DSBL_ALL	->	FORCE	-> DSBL_ALL	-> NONE
+	DSBL_ALL	->	FINISH	-> DSBL_ALL	-> END
+	DSBL_ALL	->	DISABLE	-> DSBL_ALL	-> NONE
+	DSBL_ALL	->	ENABLE	-> IDLE_ALL	-> NONE
+	DSBL_ALL	->	ALLOW	-> DSBL_ALL	-> ERROR
+	DSBL_ALL	->	FORBID	-> DSBL_FRB	-> NONE

+	DSBL_FRB	->	UPDATE	-> DSBL_FRB	-> NONE
+	DSBL_FRB	->	FORCE	-> DSBL_FRB	-> NONE
+	DSBL_FRB	->	FINISH	-> DSBL_FRB	-> END
+	DSBL_FRB	->	DISABLE	-> DSBL_FRB	-> NONE
+	DSBL_FRB	->	ENABLE	-> IDLE_FRB	-> NONE
+	DSBL_FRB	->	ALLOW	-> DSBL_ALL	-> NONE
+	DSBL_FRB	->	FORBID	-> DSBL_FRB	-> ERROR

+	IDLE_ALL	->	UPDATE	-> UPDT_ALL	-> BEGIN
+	IDLE_ALL	->	FORCE	-> UPDT_ALL	-> BEGIN
+	IDLE_ALL	->	FINISH	-> IDLE_ALL	-> END
+	IDLE_ALL	->	DISABLE	-> DSBL_ALL	-> NONE
+	IDLE_ALL	->	ENABLE	-> IDLE_ALL	-> NONE
+	IDLE_ALL	->	ALLOW	-> IDLE_ALL	-> ERROR
+	IDLE_ALL	->	FORBID	-> IDLE_FRB	-> NONE

+	IDLE_FRB	->	UPDATE	-> SCHD_FRB	-> NONE
+	IDLE_FRB	->	FORCE	-> UPDT_FRB	-> BEGIN
+	IDLE_FRB	->	FINISH	-> IDLE_FRB	-> END
+	IDLE_FRB	->	DISABLE	-> DSBL_FRB	-> NONE
+	IDLE_FRB	->	ENABLE	-> IDLE_FRB	-> NONE
+	IDLE_FRB	->	ALLOW	-> IDLE_ALL	-> NONE
+	IDLE_FRB	->	FORBID	-> IDLE_FRB	-> ERROR

+	SCHD_FRB	->	UPDATE	-> SCHD_FRB	-> NONE
+	SCHD_FRB	->	FORCE	-> SCHD_FRB_HGH	-> NONE
+	SCHD_FRB	->	FINISH	-> UPDT_FRB	-> RESTART
+	SCHD_FRB	->	DISABLE	-> JOIN_FRB	-> NONE
+	SCHD_FRB	->	ENABLE	-> SCHD_FRB	-> NONE
+	SCHD_FRB	->	ALLOW	-> UPDT_ALL	-> NONE
+	SCHD_FRB	->	FORBID	-> SCHD_FRB	-> ERROR

+	SCHD_FRB_HGH	->	UPDATE	-> SCHD_FRB_HGH	-> NONE
+	SCHD_FRB_HGH	->	FORCE	-> SCHD_FRB_HGH	-> NONE
+	SCHD_FRB_HGH	->	FINISH	-> UPDT_FRB	-> RESTART
+	SCHD_FRB_HGH	->	DISABLE	-> JOIN_FRB	-> NONE
+	SCHD_FRB_HGH	->	ENABLE	-> SCHD_FRB_HGH	-> NONE
+	SCHD_FRB_HGH	->	ALLOW	-> UPDT_ALL	-> NONE
+	SCHD_FRB_HGH	->	FORBID	-> SCHD_FRB_HGH	-> ERROR

+	UPDT_ALL	->	UPDATE	-> RSTR_ALL	-> NONE
+	UPDT_ALL	->	FORCE	-> RSTR_ALL_HGH	-> NONE
+	UPDT_ALL	->	FINISH	-> IDLE_ALL	-> END
+	UPDT_ALL	->	DISABLE	-> JOIN_ALL	-> NONE
+	UPDT_ALL	->	ENABLE	-> UPDT_ALL	-> NONE
+	UPDT_ALL	->	ALLOW	-> UPDT_ALL	-> ERROR
+	UPDT_ALL	->	FORBID	-> UPDT_FRB	-> NONE

+	UPDT_FRB	->	UPDATE	-> RSTR_FRB	-> NONE
+	UPDT_FRB	->	FORCE	-> RSTR_FRB_HGH	-> NONE
+	UPDT_FRB	->	FINISH	-> IDLE_FRB	-> END
+	UPDT_FRB	->	DISABLE	-> JOIN_FRB	-> NONE
+	UPDT_FRB	->	ENABLE	-> UPDT_FRB	-> NONE
+	UPDT_FRB	->	ALLOW	-> UPDT_ALL	-> NONE
+	UPDT_FRB	->	FORBID	-> UPDT_FRB	-> ERROR

+	RSTR_ALL	->	UPDATE	-> RSTR_ALL	-> NONE
+	RSTR_ALL	->	FORCE	-> RSTR_ALL_HGH	-> NONE
+	RSTR_ALL	->	FINISH	-> UPDT_ALL	-> RESTART
+	RSTR_ALL	->	DISABLE	-> JOIN_ALL	-> NONE
+	RSTR_ALL	->	ENABLE	-> RSTR_ALL	-> NONE
+	RSTR_ALL	->	ALLOW	-> RSTR_ALL	-> ERROR
+	RSTR_ALL	->	FORBID	-> RSTR_FRB	-> NONE

+	RSTR_FRB	->	UPDATE	-> RSTR_FRB	-> NONE
+	RSTR_FRB	->	FORCE	-> RSTR_FRB_HGH	-> NONE
+	RSTR_FRB	->	FINISH	-> UPDT_FRB	-> RESTART
+	RSTR_FRB	->	DISABLE	-> JOIN_FRB	-> NONE
+	RSTR_FRB	->	ENABLE	-> RSTR_FRB	-> NONE
+	RSTR_FRB	->	ALLOW	-> RSTR_ALL	-> NONE
+	RSTR_FRB	->	FORBID	-> RSTR_FRB	-> ERROR

+	RSTR_ALL_HGH	->	UPDATE	-> RSTR_ALL_HGH	-> NONE
+	RSTR_ALL_HGH	->	FORCE	-> RSTR_ALL_HGH	-> NONE
+	RSTR_ALL_HGH	->	FINISH	-> UPDT_ALL	-> RESTART
+	RSTR_ALL_HGH	->	DISABLE	-> JOIN_ALL	-> NONE
+	RSTR_ALL_HGH	->	ENABLE	-> RSTR_ALL_HGH	-> NONE
+	RSTR_ALL_HGH	->	ALLOW	-> RSTR_ALL_HGH	-> ERROR
+	RSTR_ALL_HGH	->	FORBID	-> RSTR_FRB_HGH	-> NONE

+	RSTR_FRB_HGH	->	UPDATE	-> RSTR_FRB_HGH	-> NONE
+	RSTR_FRB_HGH	->	FORCE	-> RSTR_FRB_HGH	-> NONE
+	RSTR_FRB_HGH	->	FINISH	-> UPDT_FRB	-> RESTART
+	RSTR_FRB_HGH	->	DISABLE	-> JOIN_FRB	-> NONE
+	RSTR_FRB_HGH	->	ENABLE	-> RSTR_FRB_HGH	-> NONE
+	RSTR_FRB_HGH	->	ALLOW	-> RSTR_ALL_HGH	-> NONE
+	RSTR_FRB_HGH	->	FORBID	-> RSTR_FRB_HGH	-> ERROR

+	JOIN_ALL	->	UPDATE	-> JOIN_ALL	-> NONE
+	JOIN_ALL	->	FORCE	-> JOIN_ALL	-> NONE
+	JOIN_ALL	->	FINISH	-> DSBL_ALL	-> END
+	JOIN_ALL	->	DISABLE	-> JOIN_ALL	-> NONE
+	JOIN_ALL	->	ENABLE	-> UPDT_ALL	-> NONE
+	JOIN_ALL	->	ALLOW	-> JOIN_ALL	-> ERROR
+	JOIN_ALL	->	FORBID	-> JOIN_FRB	-> NONE

+	JOIN_FRB	->	UPDATE	-> JOIN_FRB	-> NONE
+	JOIN_FRB	->	FORCE	-> JOIN_FRB	-> NONE
+	JOIN_FRB	->	FINISH	-> DSBL_FRB	-> END
+	JOIN_FRB	->	DISABLE	-> JOIN_FRB	-> NONE
+	JOIN_FRB	->	ENABLE	-> UPDT_FRB	-> NONE
+	JOIN_FRB	->	ALLOW	-> JOIN_ALL	-> NONE
+	JOIN_FRB	->	FORBID	-> JOIN_FRB	-> ERROR
}




enum boolean {true,false}

sig Trace {
	state 		: State,
	event 		: Event,
	transition 	: Action,
	process		: boolean,
        allowed		: boolean
}

pred transition[ t, t' : Trace ] {

	let info = states[t.state][t.event],
	    nextState = info.univ,
	    action = univ.info {
		one action implies {
			t'.state = nextState
			t.transition = action
			action = BEGIN => {
				t'.process=true 
			} else {
				action=END =>  {
					t'.process = false 
				} else { 
					t'.process= t.process
				}
			}
		} else {
			t.transition = ERROR
			t'.state = t.state
			t'.process = t.process
		}

		t.event = ALLOW implies {
			t'.allowed = true
		} else t.event = FORBID implies {
			t'.allowed = false
		} else {
			t'.allowed = t.allowed
		}
	}
}

fact trace {
	first.state = DSBL_FRB
	first.process = false
	first.allowed = false
	first.event not in (FINISH)

	all t : Trace-last, t': t.next {
		transition[t,t']
	}
}
fact finishOnlyWhenProcessActive {
	no t : Trace | t.event = FINISH and t.process=false
}
fact balancedForbidAllow {
	all t : Trace-last | t.event = ALLOW => t.allowed=false
	all t : Trace-last | t.event = FORBID => t.allowed=true
}

run Complete {
	states.univ.univ.univ = State
	univ.states.univ.univ = Event
	univ.(univ.states.univ) = State
	univ.(univ.(univ.states)) = Action

} expect 1

check NoErrors {
	no t : Trace -last |  t.transition = ERROR
} for 30

run FalseStart {  
	some  t : Trace - last | t.transition = BEGIN && t.process = true
} for 50 Trace expect 0

run Example {
} for 20
