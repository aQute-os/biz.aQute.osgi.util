package biz.aQute.gogo.commands.dtoformatter;

public class Wrapper {
	public Wrapper(Object o) {
		this.whatever = o;
	}

	public Object whatever;

	@Override
	public String toString() {
		return "[" + whatever + "]";
	}

}
