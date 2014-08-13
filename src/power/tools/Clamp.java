package power.tools;

import org.w3c.dom.Node;

import power.helpers.XmlTools;

public class Clamp implements IAdjuster {

	private static final Clamp defaultClamp = new Clamp(0, Double.POSITIVE_INFINITY);
	private static final Clamp unitClamp = new Clamp(0, 1);
	
	public final double minValue;
	public final double maxValue;
	
	private enum XmlNode { Max, Min };
	
	public static Clamp create(Node xml) {
		if (xml.hasChildNodes()) {
			double minValue = Double.NEGATIVE_INFINITY;
			Node node = XmlTools.getUptoOneNode(xml, XmlNode.Min);
			if (node != null) {
				try {
					minValue = Double.parseDouble(XmlTools.getAttributeValue(node, XmlTools.XmlAttribute.value));
				} catch (Exception e) { } // poor design
			}
			
			double maxValue = Double.POSITIVE_INFINITY;
			node = XmlTools.getUptoOneNode(xml, XmlNode.Max);
			if (node != null) {
				try {
					maxValue = Double.parseDouble(XmlTools.getAttributeValue(node, XmlTools.XmlAttribute.value));
				} catch (Exception e) { } // poor design
			}
			
			if (minValue == Double.NEGATIVE_INFINITY && maxValue == Double.POSITIVE_INFINITY) {
				return null;
			} else if (minValue == Double.NaN || maxValue == Double.NaN) {
				return null;
			} else {
				return new Clamp(minValue, maxValue);
			}
		} else {
			return defaultClamp;
		}
	}
	
	public Clamp(double minValue, double maxValue) {
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	
	@Override
	public double adjust(double value) {
		if (value > maxValue) {
			return maxValue;
		} else if (value < minValue) {
			return minValue;
		}
		return value;
	}

	@Override
	public int adjust(int value) {
		if (value > maxValue) {
			return (int)maxValue;
		} else if (value < minValue) {
			return (int)minValue;
		}
		return value;
	}

	public static Clamp getDefault() {
		return defaultClamp;
	}
	
	public static Clamp getUnit() {
		return unitClamp;
	}

	@Override
	public String description() {
		return description(0);
	}

	@Override
	public String description(int nestingLevel) {
		String tabbing = "";
		if (nestingLevel > 0) tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
		
		String str = "Clamp: {\n\t" +
				tabbing + "minValue: " + minValue + "\n\t" +
				tabbing + "maxValue: " + maxValue + "\n" +
			tabbing + "}\n";
		return str;
	}
}
