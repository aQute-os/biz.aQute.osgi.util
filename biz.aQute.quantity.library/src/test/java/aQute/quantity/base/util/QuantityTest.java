package aQute.quantity.base.util;

import aQute.quantity.types.util.Length;
import aQute.quantity.types.util.Time;
import junit.framework.TestCase;

public class QuantityTest extends TestCase {

	public void testBase() {
		Length.from(100).kilo();
		Length.from(299792458).div(Time.ONE);

	}

}