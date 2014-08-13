package power.models.cores;

import org.w3c.dom.Node;

import power.SmartGridBuilder;
import power.helpers.XmlTools;

public class ConstantModel extends Model {
	private enum XmlNode { Constant };
	
	private final double value;

	public static ConstantModel create(Node xml) {
		double value = 0;
		Node node = XmlTools.getUptoOneNode(xml, XmlNode.Constant);
		if (node != null) {
			value = Double.parseDouble(XmlTools.getAttributeValue(node, XmlTools.XmlAttribute.value));
		}
		
		return new ConstantModel(value);
	}
	
	public ConstantModel(double value) {
		this.value = value;
	}
	
	@Override
	public double getMeanSeedValue() {
		return value;
	}
	
	@Override
	public double getValue() {
		return value;
	}
	
	@Override
	public String description(int nestingLevel) {
		String tabbing = "";
		if (nestingLevel > 0) tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
		String str = "ConstantModel: {\n\t" +
				tabbing + "value: " + value + "\n" + 
				tabbing + "}: " + super.description(nestingLevel);
		return str;
	}
}
