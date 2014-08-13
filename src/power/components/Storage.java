package power.components;

import power.tools.IDescribable;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;

/**
 * @author That
 * 
 */
public class Storage implements IDescribable {

	private static Storage defaulStorage = new Storage(BlankReliability.getBlankReliability(), 0, 1, 1, 0);
	private final IReliability reliability;

	/**
	 * The amount of power currently in the system
	 */
	private double currentPower;
	private double power;

	private double currentPowerAdded;
	private double currentPowerRemoved;

	/**
	 * the capacity of the system
	 */
	private final double capacity;

	/**
	 * a value from [0, 1] indicating the percentage of power stored when
	 * charging
	 */
	private final double efficiency;

	/**
	 * a value from [0, 1] indicating the percentage of power lost per unit time
	 */
	private final double retention;

	private final double costFactor;

	public Storage(IReliability reliability, double capacity, double efficiency, double retention, double costFactor) {
		if (reliability == null) {
			this.reliability = BlankReliability.getBlankReliability();
		} else {
			this.reliability = reliability;
		}
		this.capacity = capacity;
		this.efficiency = efficiency;
		this.retention = retention;
		this.costFactor = costFactor;

		// Extras
		ScheduleParameters scheduleParams = ScheduleParameters.createRepeating(0, 1, 1);
		RunEnvironment.getInstance().getCurrentSchedule().schedule(scheduleParams, this, "initialize");

		scheduleParams = ScheduleParameters.createRepeating(0, 1, -1);
		RunEnvironment.getInstance().getCurrentSchedule().schedule(scheduleParams, this, "finalize");
	}

	public void initialize() {
		currentPowerAdded = 0;
		currentPowerRemoved = 0;
	}

	public void finalize() {
		if (currentPower > 0) {
			currentPower *= retention;
		}
		power = currentPower;
	}

	/**
	 * @param power
	 *            the amount of power which is attempted to be added to the
	 *            storage system
	 * @return the amount of power which was used to charge the battery
	 */
	public double energize(double power) {
		if (!reliability.isOperational())
			return 0.0;
		if (this.currentPower + power * this.efficiency <= this.capacity) {
			this.currentPowerAdded += power * this.efficiency;
			this.currentPower += power * this.efficiency;
			return power;
		} else {
			double powerUsed = (this.capacity - this.currentPower) / this.efficiency;
			this.currentPowerAdded += powerUsed;
			this.currentPower = this.capacity;
			return powerUsed;
		}
	}

	/**
	 * @param power
	 *            the amount of power being requested from the storage system
	 * @return the amount of power provided by the storage system
	 */
	public double draw(double power) {
		if (!reliability.isOperational())
			return 0.0;
		if (this.currentPower > power) {
			this.currentPowerRemoved += power;
			this.currentPower -= power;
			return power;
		} else {
			return drain();
		}
	}

	/**
	 * @return returns all the power in the storage system
	 */
	private double drain() {
		if (!reliability.isOperational())
			return 0.0;
		double currentPower = this.currentPower;
		this.currentPowerRemoved += currentPower;
		this.currentPower = 0;
		return currentPower;
	}

	public double getCurrentPower() {
		if (!reliability.isOperational())
			return 0.0;
		return currentPower;
	}

	public double getPower() {
		return power;
	}

	public double getCapacity() {
		if (!reliability.isOperational())
			return 0.0;
		return capacity;
	}

	public double getCurrentAvailableCapacity() {
		if (!reliability.isOperational())
			return 0.0;
		return capacity - currentPower;
	}

	public double getAvailableCapacity() {
		if (!reliability.isOperational())
			return 0.0;
		return capacity - power;
	}

	public double getEfficiency() {
		return efficiency;
	}

	public double getRetention() {
		return retention;
	}

	public double getCostFactor() {
		return costFactor;
	}

	public static Storage getDefaultStorage() {
		return defaulStorage;
	}

	public double getCurrentPowerAdded() {
		return currentPowerAdded;
	}

	public double getCurrentPowerRemoved() {
		return currentPowerRemoved;
	}

	public double getCurrentChangeInPower() {
		return getCurrentPowerAdded() - getCurrentPowerRemoved();
	}

	@Override
	public String description() {
		return this.description(0);
	}

	@Override
	public String description(int nestingLevel) {
		String tabbing = "";
		if (nestingLevel > 0)
			tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
		String str = "Storage: {\n\t" + tabbing + "capacity: " + capacity + "\n\t" + tabbing + "efficiency: " + efficiency + "\n\t" + tabbing + "retention: " + retention + "\n" + tabbing + "}\n";
		return str;
	}
}
