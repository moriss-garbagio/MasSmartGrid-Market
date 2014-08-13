package power.models;

import org.w3c.dom.Node;

import cern.jet.random.Normal;

import power.components.BlankReliability;
import power.components.IReliability;
import power.components.Reliability;
import power.helpers.XmlTools;
import power.models.cores.IModel;
import power.models.cores.Model;
import power.tools.Adjuster;
import power.tools.IAdjuster;
import power.tools.IDescribable;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;

public class RandomModel implements IRandomModel {

	protected enum XmlNode {
		Mean, MeanFactor, StandardDeviation, CostFactor, Adjuster
	};

	protected final IModel model;
	protected final IReliability reliability;

	protected final double standardDeviation;
	protected final double meanValue;
	protected final IAdjuster adjuster;

	protected final String name;
	protected final double costFactor;

	public static RandomModel create(Node xml) {
		if (xml == null)
			return null;

		IModel model = Model.create(XmlTools.getExactlyOneNode(xml, RandomModel.XmlNode.Mean));

		IReliability reliability = Reliability.create(xml);

		double meanFactor = 0;
		Node node = XmlTools.getUptoOneNode(xml, XmlNode.MeanFactor);
		if (node != null) {
			meanFactor = Double.parseDouble(XmlTools.getAttributeValue(node, XmlTools.XmlAttribute.value));
		}

		double standardDeviation = 0;
		node = XmlTools.getUptoOneNode(xml, XmlNode.StandardDeviation);
		if (node != null) {
			standardDeviation = Double.parseDouble(XmlTools.getAttributeValue(node, XmlTools.XmlAttribute.value));
		}

		double costFactor = 0;
		node = XmlTools.getUptoOneNode(xml, XmlNode.CostFactor);
		if (node != null) {
			costFactor = Double.parseDouble(XmlTools.getAttributeValue(node, XmlTools.XmlAttribute.value));
		}

		IAdjuster adjuster = Adjuster.createAll(XmlTools.getAllNodes(xml, XmlNode.Adjuster));

		String name = xml.getNodeName();

		return new RandomModel(model, reliability, meanFactor, standardDeviation, costFactor, adjuster, name);
	}

	public RandomModel(IModel model,
			IReliability reliability,
			double meanValue,
			double standardDeviation,
			double costFactor,
			IAdjuster adjuster,
			String debugID) {
		// DEBUG:
		this.name = debugID;

		this.model = model;
		if (reliability == null) {
			this.reliability = BlankReliability.getBlankReliability();
		} else {
			this.reliability = reliability;
		}
		this.adjuster = adjuster;

		this.meanValue = meanValue;
		this.standardDeviation = standardDeviation;

		this.costFactor = costFactor;
		
		ScheduleParameters scheduleParams = ScheduleParameters.createRepeating(0, 1, ScheduleParameters.FIRST_PRIORITY);
		RunEnvironment.getInstance().getCurrentSchedule().schedule(scheduleParams, this, "reset");
	}
	
	public void reset() {
		currentValue = null;
	}

	@Override
	public double getMeanSeedValue() {
		// TODO: The computed value does not consider the adjusters and is
		// inaccurate
		return model.getMeanSeedValue() * meanValue;
	}

	private Double currentValue;

	@Override
	public double getCurrentValue() {
		// TODO: allow a default value to be returned when system has failed
		if (!reliability.isOperational())
			return 0.0;

		if (currentValue == null) {
			currentValue = Normal.staticNextDouble(model.getValue(), standardDeviation) * meanValue;

			if (adjuster != null) {
				currentValue = adjuster.adjust(currentValue);
			}
		}
		return currentValue;
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
		String str = "RandomModel: {\n\t" + tabbing + "meanValue: " + meanValue + "\n\t" + tabbing + "standardDeviation: " + standardDeviation + "\n\t" + tabbing + "model: " + (model == null ? "null\n" : ((IDescribable) model).description(nestingLevel + 1)) + tabbing + "}\n";
		return str;
	}

	@Override
	public IModel getModel() {
		return model;
	}

	@Override
	public double getCostFactor() {
		return costFactor;
	}
}
