package power.distributions;

import org.w3c.dom.Node;

import cern.jet.random.AbstractDistribution;

import power.helpers.XmlTools;
import power.tools.Adjuster;
import power.tools.IAdjuster;
import power.tools.IDescribable;
import repast.simphony.random.RandomHelper;

public class LogNormalDistribution implements IRandomDistribution, IDescribable {
	private enum XmlNode {
		Adjuster, Mean, StandardDeviation
	}

	private final AbstractDistribution abstractDistribution;
	private final IAdjuster adjuster;

	public static IRandomDistribution create(Node xml) {
		if (xml == null)
			return null;
		IAdjuster adjuster = Adjuster.createAll(XmlTools.getAllNodes(xml, XmlNode.Adjuster));
		double mean = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Mean), XmlTools.XmlAttribute.value));
		double standardDeviation = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.StandardDeviation), XmlTools.XmlAttribute.value));

		return new LogNormalDistribution(mean, standardDeviation, adjuster);
	}

	public LogNormalDistribution(double mean, double standardDeviation, IAdjuster adjuster) {
		this.abstractDistribution = RandomHelper.createNormal(Math.log(mean), Math.log(standardDeviation));
		this.adjuster = adjuster;
	}

	@Override
	public double nextDouble() {
		if (adjuster != null) {
			return adjuster.adjust(Math.exp(abstractDistribution.nextDouble()));
		} else {
			return Math.exp(abstractDistribution.nextDouble());
		}
	}

	@Override
	public int nextInt() {
		if (adjuster != null) {
			return (int) adjuster.adjust(Math.exp(abstractDistribution.nextDouble()));
		} else {
			return (int) Math.exp(abstractDistribution.nextDouble());
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
		String str = "LogNormalDistribution: {\n\t" + tabbing + "Note: square the mean and standardDeviation\n\t" + tabbing + "abstractDistribution: " + abstractDistribution + "\n\t" + tabbing + "adjuster: " + (adjuster == null ? "null\n" : adjuster.description(nestingLevel + 1)) + tabbing + "}\n";
		return str;
	}
}
