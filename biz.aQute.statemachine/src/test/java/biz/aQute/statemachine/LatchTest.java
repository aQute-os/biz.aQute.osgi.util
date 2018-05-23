package biz.aQute.statemachine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import biz.aQute.statemachine.Latch.Member;

public class LatchTest {

	@Test
	public void testSimple() {
		Latch l = new Latch() {

			@Override
			protected void enable() {
				System.out.println("enable");
			}

			@Override
			protected void disable() {
				System.out.println("disable");
			}
		};

		assertTrue(l.isEnabled());

		Member m1 = l.create();
		assertFalse(l.isEnabled());

		Member m2 = l.create();
		assertFalse(l.isEnabled());

		m1.enable(true);
		assertFalse(l.isEnabled());

		m2.enable(true);
		assertTrue(l.isEnabled());
	}
}
