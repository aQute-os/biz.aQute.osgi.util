package biz.aQute.scheduler.api;

/**
 * This is a singleton exception instance to mark a canceled promise.
 */

public class CancelException extends RuntimeException {
	private static final long	serialVersionUID	= 1L;

	private CancelException() {}

	/**
	 * The singleton Cancel Exception instance
	 */
	public static CancelException	SINGLETON	= new CancelException();
}
