package power.components;

import org.w3c.dom.Node;

import power.SmartGridBuilder;
import power.distributions.IRandomDistribution;
import power.distributions.RandomDistribution;
import power.helpers.XmlTools;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;

public class Reliability implements IReliability {

	private enum XmlNode {
		Failure, Repair
	}

	private final IRandomDistribution failureDistribution;
	private final IRandomDistribution repairDistribution;

	private double timeToFailure;
	private double timeToRepair;

	public static IReliability create(Node xml) {
		if (xml == null)
			return null;

		IRandomDistribution failureDistribution = RandomDistribution.create(XmlTools.getUptoOneNode(xml, XmlNode.Failure));
		IRandomDistribution repairDistribution = RandomDistribution.create(XmlTools.getUptoOneNode(xml, XmlNode.Repair));

		if (failureDistribution == null || repairDistribution == null) {
			return BlankReliability.getBlankReliability();
		} else {
			return new Reliability(failureDistribution, repairDistribution);
		}
	}

	public Reliability(IRandomDistribution failureDistribution, IRandomDistribution durationDistribution) {
		this.failureDistribution = failureDistribution;
		this.repairDistribution = durationDistribution;

		ScheduleParameters scheduleParams = ScheduleParameters.createRepeating(0, 1, ScheduleParameters.FIRST_PRIORITY);
		RunEnvironment.getInstance().getCurrentSchedule().schedule(scheduleParams, this, "simulate");
	}

	@Override
	public boolean isOperational() {
		if (!SmartGridBuilder.getSimulateReliability())
			return true;
		return timeToFailure > 0;
	}

	public void simulate() {
		if (!SmartGridBuilder.getSimulateReliability())
			return;

		if (timeToFailure > 0) {
			timeToFailure--;
		} else {
			if (timeToRepair > 0) {
//				System.out.println("Repaining: fail in:" + timeToFailure + ", repair in: " + timeToRepair);
				timeToRepair--;
			} else {
				timeToFailure += failureDistribution.nextDouble();
				timeToRepair += repairDistribution.nextDouble();
				System.out.println("Reset: fail in:" + timeToFailure + ", repair in: " + timeToRepair);
			}
		}
	}

	@Override
	public String description() {
		return description(0);
	}

	@Override
	public String description(int nestingLevel) {
		String tabbing = "";
		if (nestingLevel > 0)
			tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
		String str = "Reliability: {\n\t" + tabbing + "failureDistribution: " + (failureDistribution == null ? "null\n" : failureDistribution.description(nestingLevel + 1)) + "\t" + tabbing + "repairDistribution: " + (repairDistribution == null ? "null\n" : repairDistribution.description(nestingLevel + 1)) + tabbing + "}\n";
		return str;
	}
}
