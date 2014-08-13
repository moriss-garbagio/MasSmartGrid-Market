package power.components.generators;

import org.w3c.dom.Node;

import power.components.IReliability;
import power.components.Reliability;

public class ReliabilityGenerator implements IGenerator<IReliability> {
	private Node xml;
	public ReliabilityGenerator(Node xml) {
		this.xml = xml;
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
		String str = "ReliabilityGenerator: {\n\t" + 
				tabbing + "xml: " + xml.toString() +
				tabbing + "}\n";
		return str; 
	}
	
	@Override
	public IReliability create() {
		return Reliability.create(xml);
	}
}
