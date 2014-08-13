package power.components;

import org.w3c.dom.Node;

import power.SmartGridBuilder;
import power.helpers.XmlTools;

public abstract class PowerPlant implements IPowerPlant {
	public enum LearningType {
		QLearning, StupidLearning
	}
	
	public enum XmlAttribute {
		learning
	}

	public static PowerPlant create(Node xml) {
		LearningType plantType = XmlTools.getAttributeValue(xml, XmlAttribute.learning, LearningType.class);
		PowerPlant plant;
		if (plantType == null) {
			plant = StupidLearningPlant.create(xml);
		} else {
			
			switch (plantType) {
			case QLearning:
				plant = QLearningPlant.create(xml);
				break;
			case StupidLearning:
			default:
				plant = StupidLearningPlant.create(xml);
			}
		}
		
		return plant;
	}
}
