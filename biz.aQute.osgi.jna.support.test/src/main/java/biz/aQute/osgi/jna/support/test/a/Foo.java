package biz.aQute.osgi.jna.support.test.a;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

@FieldOrder({"text"})
public class Foo extends Structure implements Structure.ByReference {
	public String text;
}