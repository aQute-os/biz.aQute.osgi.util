package biz.aQute.statemachine;

/**
 * Provides a latch if you need a common decision absed on multiple inputs. This
 * latch can have a number of members that enable or disable the latch. The latch is,
 * however, only really really enabled if all members agree that it should be enabled. 
 */
public abstract class Latch {
	int				counter;
	boolean			enabled	= true;
	final Object	lock	= new Object();

	/**
	 * Private object for a member
	 */
	public interface Member {
		void enable(boolean enable);
	}

	Member create() {

		forbid();

		return new Member() {
			boolean currentMemberState = false;

			@Override
			public void enable(boolean enable) {
				synchronized (lock) {
					if (currentMemberState == enable)
						return;

					currentMemberState = enable;

					if (enable) {
						allow();
					} else {
						forbid();
					}
				}
			}
		};
	}

	public synchronized boolean isEnabled() {
		return enabled;
	}

	private void allow() {
		counter--;
		if (counter == 0) {
			enabled = true;
			enable();
		}
	}

	private void forbid() {
		counter++;
		if (counter == 1) {
			enabled = false;
			disable();
		}
	}

	abstract protected void enable();

	abstract protected void disable();
}
