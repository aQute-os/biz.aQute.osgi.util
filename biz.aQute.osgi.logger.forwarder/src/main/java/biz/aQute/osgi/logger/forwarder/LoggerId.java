package biz.aQute.osgi.logger.forwarder;

import java.util.Objects;

import org.osgi.framework.Bundle;

final class LoggerId {
	final Bundle	bundle;
	final String	name;

	LoggerId(Bundle bundle, String name) {
		this.bundle = bundle;
		this.name = name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bundle, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final LoggerId other = (LoggerId) obj;
		return Objects.equals(bundle, other.bundle) && Objects.equals(name, other.name);
	}

}
