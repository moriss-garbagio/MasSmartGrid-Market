package power.models.cores;

import java.util.ArrayList;

import org.w3c.dom.Node;

import power.helpers.XmlTools;
import power.tools.Adjuster;
import power.tools.IAdjuster;

public class ExponentialDynamicFunctionModel extends DynamicModel {
	private enum XmlNode { A, B, C, D, E, DynamicFunctionList, DynamicFunction, Adjuster }
	
	private final double a;
	private final double b;
	private final double c;
	private final double d;
	private final double e;
	
	public static ExponentialDynamicFunctionModel create(Node xml) {
		if (xml == null) return null;
		String value = null;
		
		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.A), XmlTools.XmlAttribute.value);
		double a = value != null ? Double.parseDouble(value) : 0;
		
		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.B), XmlTools.XmlAttribute.value);
		double b = value != null ? Double.parseDouble(value) : 0;
		
		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.C), XmlTools.XmlAttribute.value);
		double c = value != null ? Double.parseDouble(value) : 0;
		
		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.D), XmlTools.XmlAttribute.value);
		double d = value != null ? Double.parseDouble(value) : 0;
		
		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.E), XmlTools.XmlAttribute.value);
		double e = value != null ? Double.parseDouble(value) : 0;
		
		ArrayList<Node> nodeList = XmlTools.getAtLeastOneNode(XmlTools.getExactlyOneNode(xml, XmlNode.DynamicFunctionList), XmlNode.DynamicFunction);
		Property[] propertyList = new Property[nodeList.size()];
		for (int index = 0; index < propertyList.length; index++)
		{
			String property = XmlTools.getAttributeValue(nodeList.get(index), XmlTools.XmlAttribute.value);
			IAdjuster adjuster = Adjuster.createAll(XmlTools.getAllNodes(nodeList.get(index), XmlNode.Adjuster));
			propertyList[index] = new Property(property, adjuster);
		}
		
		return new ExponentialDynamicFunctionModel(a, b, c, d, e, propertyList);
	}
	
	public ExponentialDynamicFunctionModel(double a, double b, double c, double d, double e, Property[] propertyList) {
		super(propertyList);
		
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
	}		
	
	public double getValue() {
		double value = getPropertyValue(0);
		return a * Math.exp(b * value) + c * Math.exp(d * value) + e;
	}
	
	@Override
	public String description(int nestingLevel) {
		String tabbing = "";
		if (nestingLevel > 0) tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
		String str = "ExponentialFunctionModel: {\n\t" +
				tabbing + "source: " + getSource().toString() + "\n\t" +
				tabbing + "a: " + a + "\n\t" +
				tabbing + "b: " + b + "\n\t" +
				tabbing + "c: " + c + "\n\t" +
				tabbing + "d: " + d + "\n\t" +
				tabbing + "e: " + e + "\n" +
			tabbing + "}\n";
		return str;
	}
}
