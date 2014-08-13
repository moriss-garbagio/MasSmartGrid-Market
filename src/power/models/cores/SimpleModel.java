package power.models.cores;

import org.w3c.dom.Node;

import power.helpers.XmlTools;
import power.distributions.IRandomDistribution;
import power.distributions.RandomDistribution;

public class SimpleModel extends Model {
	private enum XmlNode { Distribution }

	private final IRandomDistribution distribution;

	public static SimpleModel create(Node xml) {
		IRandomDistribution distribution = RandomDistribution.create(XmlTools.getExactlyOneNode(xml, XmlNode.Distribution));
		return new SimpleModel(distribution);
	}
	
	public SimpleModel(IRandomDistribution distribution) {
		this.distribution = distribution;
	}
	
	@Override
	public double getValue() {
		return distribution.nextDouble();
	}
	
	@Override
	public String description(int nestingLevel) {
		String tabbing = "";
		if (nestingLevel > 0) tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
		String str = "SimpleModel: {\n\t" +
				tabbing + "distribution: " + distribution.description(nestingLevel + 1) + "\n" + 
				tabbing + "}:" + super.description(nestingLevel);
		return str;
	}
}
