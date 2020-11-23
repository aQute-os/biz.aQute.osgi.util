package test;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import purejavacomm.CommPortIdentifier;

public class Comm {

	@Test
	public void show() {

		ArrayList<CommPortIdentifier> list = Collections.list(CommPortIdentifier.getPortIdentifiers());

		list.forEach(c -> System.out.println(c.getName() + " " + c.getPortType()));
	}

}
