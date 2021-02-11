package aQute.quantity.types.util;

import aQute.quantity.base.util.DerivedQuantity;
import aQute.quantity.base.util.Unit;
import aQute.quantity.base.util.UnitInfo;

/**
 * In mechanics, acceleration is the rate of change of the velocity of an object
 * with respect to time. Accelerations are vector quantities (in that they have
 * magnitude and direction). The orientation of an object's acceleration
 * is given by the orientation of the net force acting on that object. The
 * magnitude of an object's acceleration, as described by Newton's Second
 * Law, is the combined effect of two causes:
 * <ul>
 * <li>
 * the net balance of all external forces acting onto that object — magnitude is
 * directly proportional to this net resulting force; that object's mass,
 * depending on the materials out of which it is made — magnitude is inversely
 * proportional to the object's mass.
 * <li>
 * The SI unit for acceleration is metre per second squared (m/s<sup>2</sup>).
 * </ul>
 * For example, when a vehicle starts from a standstill (zero velocity, in an
 * inertial frame of reference) and travels in a straight line at increasing
 * speeds, it is accelerating in the direction of travel. If the vehicle turns,
 * an acceleration occurs toward the new direction and changes its motion
 * vector. The acceleration of the vehicle in its current direction of motion is
 * called a linear (or tangential during circular motions) acceleration, the
 * reaction to which the passengers on board experience as a force pushing them
 * back into their seats. When changing direction, the effecting acceleration is
 * called radial (or orthogonal during circular motions) acceleration, the
 * reaction to which the passengers experience as a centrifugal force. If the
 * speed of the vehicle decreases, this is an acceleration in the opposite
 * direction and mathematically a negative, sometimes called deceleration, and
 * passengers experience the reaction to deceleration as an inertial force
 * pushing them forward. Such negative accelerations are often achieved by
 * retrorocket burning in spacecraft.[4] Both acceleration and deceleration are
 * treated the same, they are both changes in velocity. Each of these
 * accelerations (tangential, radial, deceleration) is felt by passengers until
 * their relative (differential) velocity are neutralized in reference to the
 * vehicle.
 */
@UnitInfo(unit = "m/s²", symbol = "a", dimension = "Acceleration", symbolForDimension = "")
public class Acceleration extends DerivedQuantity<Acceleration> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Acceleration.class, Length.DIMe1, Time.DIMe_2);

	/**
	 * Constructor for  m/s<sup>2</sup>
	 * 
	 * @param value the acceleration in m/s<sup>2</sup>
	 */
	public Acceleration(double value) {
		super(value);
	}

	@Override
	protected Acceleration same(double value) {
		return fromMeterPerSecond2(value);
	}

	/**
	 * From m/s<sup>2</sup>
	 * @param value m/s<sup>2</sup>
	 * @return
	 */
	public static Acceleration fromMeterPerSecond2(double value) {
		return new Acceleration(value);
	}

	/**
	 * To m/s<sup>2</sup>
	 * @return m/s<sup>2</sup>
	 */
	public double toMeterPerSecond2() {
		return value;
	}
	
	public Acceleration g() {
		return fromMeterPerSecond2(9.80665D);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

}
