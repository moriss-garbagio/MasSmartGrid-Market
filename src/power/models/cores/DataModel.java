package power.models.cores;

import java.util.Arrays;

import org.w3c.dom.Node;

import power.SmartGridBuilder;
import power.helpers.Computations;
import power.helpers.XmlTools;

import com.google.gson.Gson;

import repast.simphony.essentials.RepastEssentials;

public class DataModel extends Model {
	private enum XmlNode { Data }
	
	private final double data[];
	
	public static DataModel create(Node xml) {
		XmlTools.FormatType formatType = XmlTools.getAttributeValue(xml, XmlTools.XmlAttribute.format, XmlTools.FormatType.class);
		double[] data = null;

		switch (formatType) {
		case xml:
			data = XmlTools.getTypedListFromNode(xml, XmlNode.Data, XmlTools.XmlAttribute.value, double[].class);
			return new DataModel(data);
		case json:
		default:
			data = (new Gson()).fromJson(xml.getTextContent(), double[].class);
			return new DataModel(data);
		}
	}
	
	public DataModel(double[] data) {
		this.data = data;
		mean = Computations.getMean(data);
	}
	
	private final double mean;
	@Override
	public double getMeanSeedValue() {
		return mean;
	}
	
	@Override
	public double getValue() {
		if (data.length > 0) {
			return data[(int)RepastEssentials.GetTickCount() % data.length];
		} else {
			return 0;
		}
	}
	
	@Override
	public String description(int nestingLevel) {
		String tabbing = "";
		if (nestingLevel > 0) tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
		String str = "DataModel: {\n\t" +
				tabbing + "source: " + source + "\n\t" + 
				tabbing + "data: " + Arrays.toString(data) + "\n" +
			tabbing + "}\n";
		return str;
	}
}