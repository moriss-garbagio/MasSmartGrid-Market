package power.models.cores;

import org.w3c.dom.Node;
import power.helpers.XmlTools;

import power.tools.IDescribable;

public abstract class Model implements IModel {
	public enum ModelType { DataModel, ExponentialDynamicFunctionModel, SimpleModel, ConstantModel }
	
	protected Object source;
	
	public static IModel create(Node xml) {
		ModelType type = XmlTools.getAttributeValue(xml, XmlTools.XmlAttribute.type, ModelType.class);
		
		switch (type) {
		case ConstantModel:
			return ConstantModel.create(xml);
		case ExponentialDynamicFunctionModel:
			return ExponentialDynamicFunctionModel.create(xml);
		case SimpleModel:
			return SimpleModel.create(xml);
		case DataModel:
		default:
			return DataModel.create(xml);
		}
	}
	
	@Override
	public double getMeanSeedValue() {
		return Double.NaN;
	}
	
	@Override
	public void setSource(Object source) {
		this.source = source;
	}

	@Override
	public Object getSource() {
		return this.source;
	}

	@Override
	public String description() {
		return this.description(0);
	}
	
	@Override
	public String description(int nestingLevel) {
		String tabbing = "";
		if (nestingLevel > 0) tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
		String str = "Model: {\n\t" +
				tabbing + "source: " + source + "\n" + 
			tabbing + "}\n";
		return str;
	}
}
