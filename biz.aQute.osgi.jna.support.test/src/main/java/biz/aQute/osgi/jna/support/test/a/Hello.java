package biz.aQute.osgi.jna.support.test.a;


import com.sun.jna.Library;

public interface Hello extends Library{

	
	void hello();
	
	Foo create();
	void close(Foo foo);
	
	void fill(Foo foo);
}
