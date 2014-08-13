package power.tools;

import java.util.ArrayList;

import org.w3c.dom.Node;
import power.helpers.XmlTools;

public class Adjuster implements IAdjuster {
	private enum AdjusterType {
		Clamp, Absolute, Amplify, Exp
	}

	IAdjuster[] adjusterList;

	// TODO: this is not general enough
	public static IAdjuster createAll(ArrayList<Node> xmlList) {
		if (xmlList == null || xmlList.isEmpty()) {
			return null;
		} else if (xmlList.size() == 1) {
			return create(xmlList.get(0));
		} else {
			ArrayList<IAdjuster> adjusterList = new ArrayList<IAdjuster>();
			for (Node node : xmlList) {
				IAdjuster adjuster = create(node);
				if (adjuster != null) {
					adjusterList.add(create(node));
				}
			}
			if (adjusterList.isEmpty()) {
				return null;
			} else if (adjusterList.size() == 1) {
				return adjusterList.get(0);
			} else {
				return new Adjuster(adjusterList.toArray(new IAdjuster[adjusterList.size()]));
			}
		}
	}

	public static IAdjuster create(Node xml) {
		if (xml == null)
			return null;

		AdjusterType adjusterType = XmlTools.getAttributeValue(xml, XmlTools.XmlAttribute.type, AdjusterType.class);
		switch (adjusterType) {
		case Absolute:
			return Absolute.getSingleton();
		case Amplify:
			return Amplify.create(xml);
		case Exp:
			return Exp.getSingleton();
		case Clamp:
		default:
			return Clamp.create(xml);
		}
	}

	public Adjuster(IAdjuster[] adjusterList) {
		this.adjusterList = adjusterList;
	}

	@Override
	public double adjust(double value) {
		if (adjusterList != null) {
			for (IAdjuster adjuster : adjusterList) {
				value = adjuster.adjust(value);
			}
			return value;
		} else {
			return value;
		}
	}

	@Override
	public int adjust(int value) {
		if (adjusterList != null) {
			for (IAdjuster adjuster : adjusterList) {
				value = adjuster.adjust(value);
			}
			return value;
		} else {
			return value;
		}
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

		String adjusterListDesc = "[";
		for (IAdjuster adjuster : adjusterList) {
			adjusterListDesc += adjuster.description(nestingLevel + 2);
		}
		adjusterListDesc += "]";

		String str = "Adjuster: {\n\t" + tabbing + "adjusterList: " + adjusterListDesc + "\n" + tabbing + "}\n";

		return str;
	}
}
