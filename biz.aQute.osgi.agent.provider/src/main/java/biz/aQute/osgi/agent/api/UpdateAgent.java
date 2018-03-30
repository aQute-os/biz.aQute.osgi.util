package biz.aQute.osgi.agent.api;

public interface UpdateAgent {
	enum State {
		ENABLED, DISABLED, DOWNLOAD_CONFIG, PREPARING, COMMITTING, FINALIZING, WAITING, STARTING, RETRY_WAIT, STOPPING;
	}

	boolean disable();

	boolean enable();
	
	
	boolean trigger();

	State getState();
}
