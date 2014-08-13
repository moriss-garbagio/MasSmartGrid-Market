package power.components.generators;

import org.w3c.dom.Node;

import power.components.IReliability;
import power.components.Storage;
import power.distributions.IRandomDistribution;
import power.distributions.RandomDistribution;
import power.helpers.XmlTools;

public class StorageGenerator implements IGenerator<Storage> {
	private enum XmlNode {
		Capacity, Efficiency, Retention, CostFactor
	}

	private final IGenerator<IReliability> reliabilityGenerator;
	private final IRandomDistribution capacityGenerator;
	private final IRandomDistribution efficiencyGenerator;
	private final IRandomDistribution retentionGenerator;
	private final IRandomDistribution costFactorGenerator;

	public static StorageGenerator create(Node xml) {
		if (xml == null)
			return null;

		IGenerator<IReliability> reliabilityGenerator = new ReliabilityGenerator(xml);

		IRandomDistribution capacityGenerator = RandomDistribution.create(XmlTools.getExactlyOneNode(xml, XmlNode.Capacity));
		IRandomDistribution efficiencyGenerator = RandomDistribution.create(XmlTools.getUptoOneNode(xml, XmlNode.Efficiency));
		IRandomDistribution retentionGenerator = RandomDistribution.create(XmlTools.getUptoOneNode(xml, XmlNode.Retention));
		IRandomDistribution costFactorGenerator = RandomDistribution.create(XmlTools.getUptoOneNode(xml, XmlNode.CostFactor));

		return new StorageGenerator(reliabilityGenerator, capacityGenerator, efficiencyGenerator, retentionGenerator, costFactorGenerator);
	}

	public StorageGenerator(IGenerator<IReliability> reliabilityGenerator,
			IRandomDistribution capacityGenerator,
			IRandomDistribution efficiencyGenerator,
			IRandomDistribution leakGenerator,
			IRandomDistribution costFactorGenerator) {

		this.reliabilityGenerator = reliabilityGenerator;

		this.capacityGenerator = capacityGenerator;
		this.efficiencyGenerator = efficiencyGenerator;
		this.retentionGenerator = leakGenerator;
		this.costFactorGenerator = costFactorGenerator;
	}

	@Override
	public Storage create() {

		IReliability reliability = null;
		if (reliabilityGenerator != null) {
			reliability = reliabilityGenerator.create();
		}

		double efficiency = 1;
		if (efficiencyGenerator != null) {
			efficiency = efficiencyGenerator.nextDouble();
		}

		double retention = 1;
		if (retentionGenerator != null) {
			retention = retentionGenerator.nextDouble();
		}

		double costFactor = 0;
		if (costFactorGenerator != null) {
			costFactor = costFactorGenerator.nextDouble();
		}

		return new Storage(reliability, capacityGenerator.nextDouble(), efficiency, retention, costFactor);
	}

	@Override
	public String description() {
		return this.description(0);
	}

	// TODO
	@Override
	public String description(int nestingLevel) {
		String tabbing = "";
		if (nestingLevel > 0)
			tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
		String str = "StorageGenerator: {\n\t" + tabbing + "reliabilityGenerator: " + (reliabilityGenerator == null ? "null\n" : reliabilityGenerator.description(nestingLevel + 1)) + "\t" + tabbing + "capacityGenerator: " + (capacityGenerator == null ? "null\n" : capacityGenerator.description(nestingLevel + 1)) + "\t" + tabbing + "efficiencyGenerator: " + (efficiencyGenerator == null ? "null\n" : efficiencyGenerator.description(nestingLevel + 1)) + "\t" + tabbing + "retentionGenerator: " + (retentionGenerator == null ? "null\n" : retentionGenerator.description(nestingLevel + 1)) + "\t" + tabbing + "costFactorGenerator: " + (costFactorGenerator == null ? "null\n" : costFactorGenerator.description(nestingLevel + 1)) + tabbing + "}\n";
		return str;
	}
}
