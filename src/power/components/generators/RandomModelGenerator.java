package power.components.generators;

import org.w3c.dom.Node;

import power.components.IReliability;
import power.distributions.IRandomDistribution;
import power.distributions.RandomDistribution;
import power.helpers.XmlTools;
import power.models.IRandomModel;
import power.models.RandomModel;
import power.models.cores.IModel;
import power.models.cores.Model;
import power.tools.Adjuster;
import power.tools.IAdjuster;

public class RandomModelGenerator implements IGenerator<IRandomModel> {
	private enum XmlNode {
		Mean, MeanFactor, StandardDeviation, PredictionError, CostFactor, Adjuster
	}

	private final IModel model;
	private final IGenerator<IReliability> reliabilityGenerator;
	private final IRandomDistribution meanFactorGenerator;
	private final IRandomDistribution standardDeviationGenerator;
	private final IRandomDistribution costFactorGenerator;

	private final IAdjuster adjuster;
	private final String name;

	public static IGenerator<IRandomModel> create(Node xml) {
		if (xml == null)
			return null;
		IModel model = Model.create(XmlTools.getExactlyOneNode(xml, XmlNode.Mean));
		IGenerator<IReliability> reliabilityGenerator = new ReliabilityGenerator(xml);

		IRandomDistribution meanFactorGenerator = RandomDistribution.create(XmlTools.getUptoOneNode(xml, XmlNode.MeanFactor));
		IRandomDistribution standardDeviationGenerator = RandomDistribution.create(XmlTools.getUptoOneNode(xml, XmlNode.StandardDeviation));
		IRandomDistribution costFactorGenerator = RandomDistribution.create(XmlTools.getUptoOneNode(xml, XmlNode.CostFactor));

		IAdjuster adjuster = Adjuster.createAll(XmlTools.getAllNodes(xml, XmlNode.Adjuster));
		String name = XmlTools.getAttributeValue(xml, XmlTools.XmlAttribute.name);
		if (name == null)
			name = xml.getNodeName();

		return new RandomModelGenerator(model, reliabilityGenerator, meanFactorGenerator, standardDeviationGenerator, costFactorGenerator, adjuster, name);
	}

	public RandomModelGenerator(IModel model,
			IGenerator<IReliability> reliabilityGenerator,
			IRandomDistribution meanValueGenerator,
			IRandomDistribution standardDeviationGenerator,
			IRandomDistribution costFactorGenerator,
			IAdjuster adjuster,
			String name) {
		this.name = name;

		this.model = model;
		this.reliabilityGenerator = reliabilityGenerator;
		this.meanFactorGenerator = meanValueGenerator;
		this.standardDeviationGenerator = standardDeviationGenerator;
		this.costFactorGenerator = costFactorGenerator;
		this.adjuster = adjuster;
	}

	@Override
	public IRandomModel create() {

		IReliability reliability = null;
		if (reliabilityGenerator != null) {
			reliability = reliabilityGenerator.create();
		}

		double meanFactor = 1;
		if (meanFactorGenerator != null) {
			meanFactor = meanFactorGenerator.nextDouble();
		}

		double standardDeviation = 0;
		if (standardDeviationGenerator != null) {
			standardDeviation = standardDeviationGenerator.nextDouble();
		}

		double costFactor = 0;
		if (costFactorGenerator != null) {
			costFactor = costFactorGenerator.nextDouble();
		}

		return new RandomModel(model, reliability, meanFactor, standardDeviation, costFactor, adjuster, name);
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
		String str = "RandomModel: {\n\t" + tabbing + "meanFactorGenerator: " + (meanFactorGenerator == null ? "null\n" : meanFactorGenerator.description(nestingLevel + 1)) + "\t" + tabbing + "model: " + (model == null ? "null\n" : model.description(nestingLevel + 1)) + "\t" + tabbing + "reliabilityGenerator: " + (reliabilityGenerator == null ? "null\n" : reliabilityGenerator.description(nestingLevel + 1)) + "\t" + tabbing + "standardDeviationGenerator: " + (standardDeviationGenerator == null ? "null\n" : standardDeviationGenerator.description(nestingLevel + 1)) + "\t" + tabbing + "costFactorGenerator: " + (costFactorGenerator == null ? "null\n" : costFactorGenerator.description(nestingLevel + 1)) + tabbing + "}\n";
		return str;
	}
}
