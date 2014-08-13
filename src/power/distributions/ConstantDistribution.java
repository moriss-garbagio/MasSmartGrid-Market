package power.distributions;

import org.w3c.dom.Node;

import power.helpers.XmlTools;
import power.tools.IDescribable;

public class ConstantDistribution implements IRandomDistribution, IDescribable {
	private enum XmlNode { Constant }
	
	private final double constant;
	
	public static IRandomDistribution create(Node xml) {
		if (xml == null) {
			return null;
		}
		Node node = XmlTools.getUptoOneNode(xml, XmlNode.Constant);
		String attribute = node == null ? null : XmlTools.getAttributeValue(node, XmlTools.XmlAttribute.value);
		if (node == null || attribute == null) {
			return new ConstantDistribution(0);
		} else {
			return new ConstantDistribution(Double.parseDouble(attribute));
		}
	}
	
	public ConstantDistribution(double constant) {
		this.constant = constant;
	}

	@Override
	public double nextDouble() {
		return constant;
	}

	@Override
	public int nextInt() {
		return (int)constant;
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
		String str = "ConstantDistribution: {\n\t" +
				tabbing + "constant: " + constant + "\n" +
				tabbing + "}\n";
		return str;
	}
}
