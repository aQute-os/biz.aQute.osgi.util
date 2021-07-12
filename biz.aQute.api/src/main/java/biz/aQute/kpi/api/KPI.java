package biz.aQute.kpi.api;

import org.osgi.dto.DTO;

/**
 * A Key Performance Indicator (KPI) is a set of values representing the ongoing state of a service implementation.
 * <p>
 * A service implementation can define a Data Transfer Object (DTO) that contains its measurements. This services
 * van ask for a snapshot of this value. The caller then owns the returned object. The DTO can also be reset.
 *
 */
public interface KPI {

	/**
	 * Reset any counters and time based values
	 */
	void reset();

	/**
	 * Return a snapshot
	 *
	 * @return a snapshot of the KPI values
	 */
	DTO snapshot();
}