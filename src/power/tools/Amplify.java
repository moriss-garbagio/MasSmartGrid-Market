package power.tools;

import org.w3c.dom.Node;

import power.helpers.XmlTools;

public class Amplify implements IAdjuster {
	private enum XmlNode {
		Factor
	};

	private static Amplify handyInstance;

	public static Amplify getHandyInstance(double factor) {
		if (handyInstance == null) {
			handyInstance = new Amplify(factor);
			return handyInstance;
		} else {
			handyInstance.setFactor(factor);
			return handyInstance;
		}
	}

	private double factor;

	public Amplify(double factor) {
		this.factor = factor;
	}

	public double getFactor() {
		return factor;
	}

	public void setFactor(double factor) {
		this.factor = factor;
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

		String str = "Amplifier: {\n\t" + tabbing + "factor: " + factor + "\n" + tabbing + "}\n";
		return str;
	}

	@Override
	public double adjust(double value) {
		return value * factor;
	}

	@Override
	public int adjust(int value) {
		return value * (int) factor;
	}

	public static IAdjuster create(Node xml) {
		double factor = 0.0;
		Node node = XmlTools.getUptoOneNode(xml, XmlNode.Factor);
		if (node == null) {
			return null;
		} else {
			try {
				factor = Double.parseDouble(XmlTools.getAttributeValue(node, XmlTools.XmlAttribute.value));
			} catch (Exception e) {
			} // poor design
		}
		return new Amplify(factor);
	}
}
