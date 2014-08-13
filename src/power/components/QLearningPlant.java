package power.components;

import org.w3c.dom.Node;

import com.google.gson.Gson;

import cern.jet.random.Empirical;
import power.SmartGridBuilder;
import power.auction.Bid;
import power.components.generators.IGenerator;
import power.components.generators.RandomModelGenerator;
import power.helpers.XmlTools;
import power.models.IRandomModel;
import power.tools.QLearning;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;

public class QLearningPlant extends PowerPlant {

	private enum XmlNode {
		A, B, C, MinQuantityGenerator, MaxQuantityGenerator, ScalingFactorSet, ProbabilitySet, ActionSet, Alpha, Gamma, Data
	};

	private String id;

	// cost function: c(q) = a*q^2 + b*q + c
	// cost function reported to the auctioneer: f(q) = m*a*q^2 + m*b*q + c
	private final double a, b, c;

	// domain of production quantity q: [q_min, q_max]
	private IRandomModel minQuantityModel;
	private IRandomModel maxQuantityModel;

	// markup rate m (m >= 1)
	// m[HOURS]
	private double[] scalingFactor;

	// action set
	// acts[n_act]
	private double[] actionList;

	// probability of selecting each action for each hour of the next day
	// p[HOURS][n_act]
	private double[] probability;

	// propensity of each actions for each action for each hour of the next day
	// Q[HOURS][n_act]
	private QLearning[] learnerList;

	private int previousAction;
	private double quantity;

	public static PowerPlant create(Node xml) {
		String name = XmlTools.getAttributeValue(xml, XmlTools.XmlAttribute.name);

		String value = null;
		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.A), XmlTools.XmlAttribute.value);
		double a = value != null ? Double.parseDouble(value) : 0;

		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.B), XmlTools.XmlAttribute.value);
		double b = value != null ? Double.parseDouble(value) : 0;

		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.C), XmlTools.XmlAttribute.value);
		double c = value != null ? Double.parseDouble(value) : 0;

		IGenerator<IRandomModel> minQuantityGenerator = RandomModelGenerator.create(XmlTools.getExactlyOneNode(xml, XmlNode.MinQuantityGenerator));
		IRandomModel minQuantityModel = minQuantityGenerator.create();
		
		IGenerator<IRandomModel> maxQuantityGenerator = RandomModelGenerator.create(XmlTools.getExactlyOneNode(xml, XmlNode.MaxQuantityGenerator));
		IRandomModel maxQuantityModel = maxQuantityGenerator.create();
		
//		value = XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.MinQuantity), XmlTools.XmlAttribute.value);
//		double minQuantity = value != null ? Double.parseDouble(value) : Double.NEGATIVE_INFINITY;
//
//		value = XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.MaxQuantity), XmlTools.XmlAttribute.value);
//		double maxQuantity = value != null ? Double.parseDouble(value) : Double.POSITIVE_INFINITY;
		
		value = XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Alpha), XmlTools.XmlAttribute.value);
		double alpha = value != null ? Double.parseDouble(value) : 0.5;

		value = XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Gamma), XmlTools.XmlAttribute.value);
		double gamma = value != null ? Double.parseDouble(value) : 0.5;

		Node node = null;
		XmlTools.FormatType formatType = null;
		node = XmlTools.getUptoOneNode(xml, XmlNode.ScalingFactorSet);
		double[] scalingFactorList = null;
		if (node == null) {
			System.out.println("powerplant " + name + " is missing initial scaling factors set; initializing to defaults.");
			scalingFactorList = new double[(int) SmartGridBuilder.getPeriod()];
			for (int i = 0; i < scalingFactorList.length; i++) {
				scalingFactorList[i] = 1.0;
			}
		} else {
			formatType = XmlTools.getAttributeValue(node, XmlTools.XmlAttribute.format, XmlTools.FormatType.class);
			switch (formatType) {
			case xml:
				scalingFactorList = XmlTools.getTypedListFromNode(node, XmlNode.Data, XmlTools.XmlAttribute.value, double[].class);
			case json:
			default:
				scalingFactorList = (new Gson()).fromJson(node.getTextContent(), double[].class);
			}
		}

		node = XmlTools.getUptoOneNode(xml, XmlNode.ActionSet);
		double[] actionList = null;
		if (node == null) {
			System.out.println("powerplant " + name + " is missing an action set; initializing to defaults.");
			actionList = new double[] { -0.1, 0.0, 0.1 };
		} else {
			formatType = XmlTools.getAttributeValue(node, XmlTools.XmlAttribute.format, XmlTools.FormatType.class);
			switch (formatType) {
			case xml:
				actionList = XmlTools.getTypedListFromNode(node, XmlNode.Data, XmlTools.XmlAttribute.value, double[].class);
			case json:
			default:
				actionList = (new Gson()).fromJson(node.getTextContent(), double[].class);
			}
		}

		node = XmlTools.getUptoOneNode(xml, XmlNode.ProbabilitySet);
		double[] probabilityList = null;
		if (formatType == null) {
			System.out.println("powerplant " + name + " is missing initial probobility set; initializing to defaults.");
			probabilityList = new double[actionList.length];
			for (int i = 0; i < probabilityList.length; i++) {
				probabilityList[i] = 1.0 / actionList.length;
			}
		} else {
			formatType = XmlTools.getAttributeValue(node, XmlTools.XmlAttribute.format, XmlTools.FormatType.class);
			switch (formatType) {
			case xml:
				probabilityList = XmlTools.getTypedListFromNode(node, XmlNode.Data, XmlTools.XmlAttribute.value, double[].class);
			case json:
			default:
				probabilityList = (new Gson()).fromJson(node.getTextContent(), double[].class);
			}
		}

		return new QLearningPlant(name, a, b, c, minQuantityModel, maxQuantityModel, alpha, gamma, scalingFactorList, probabilityList, actionList);
	}

	// constructor
	private QLearningPlant(String name,
			double a,
			double b,
			double c,
			IRandomModel minQuantityModel,
			IRandomModel maxQuantityModel,
			double alpha,
			double gamma,
			double[] scalingFactorList,
			double[] probabilityList,
			double[] actionList) {

		this.id = name;
		this.a = a;
		this.b = b;
		this.c = c;
		this.scalingFactor = scalingFactorList;
		this.minQuantityModel = minQuantityModel;
		this.maxQuantityModel = maxQuantityModel;
		this.actionList = actionList;

		probability = new double[actionList.length];
		for (int i = 0; i < probability.length; i++) {
			probability[i] = 1.0 / actionList.length;
		}

		learnerList = new QLearning[(int) SmartGridBuilder.getPeriod()];
		for (int i = 0; i < learnerList.length; i++) {
			learnerList[i] = new QLearning(1, actionList.length, alpha, gamma);
		}
	}

	@Override
	public void execute(double quota) {
//		System.err.println(quota);
		this.quantity = quota;
		bid = null;
		int hour = ((int) RepastEssentials.GetTickCount()) % SmartGridBuilder.getPeriod();
		learnerList[hour].learn(0, previousAction, 0, reward());
		double qsum = 0;
		for (int i = 0; i < actionList.length; i++) {
			qsum += learnerList[((int) RepastEssentials.GetTickCount()) % SmartGridBuilder.getPeriod()].getQValue(0, i);
		}
		for (int i = 0; i < probability.length; i++) {
			probability[i] = learnerList[((int) RepastEssentials.GetTickCount()) % SmartGridBuilder.getPeriod()].getQValue(0, i) / qsum;
		}
	}

	// send bid to auctioneer
	Bid bid = null;

	@Override
	public Bid getBid() {
		if (bid == null) {
			int hour = ((int) RepastEssentials.GetTickCount()) % SmartGridBuilder.getPeriod();
			previousAction = (int) Math.round(RandomHelper.createEmpirical(probability, Empirical.NO_INTERPOLATION).nextDouble() * actionList.length);
			scalingFactor[hour] *= 1 + actionList[previousAction];
//			System.err.println("m[" + hour + "]: " + scalingFactor[hour]);
			double minQuantity = minQuantityModel.getCurrentValue();
			double maxQuantity = maxQuantityModel.getCurrentValue();
			if (minQuantity > maxQuantity) {
				maxQuantity = minQuantity = (minQuantity + maxQuantity) / 2;
			}
			bid = new Bid(scalingFactor[hour] * a, scalingFactor[hour] * b, minQuantity, maxQuantity);
		}
		return bid;
	}
	
	@Override
	public double getQuantity() {
		return quantity;
	}
	
	@Override
	public double reward() {
		int hour = ((int) RepastEssentials.GetTickCount()) % SmartGridBuilder.getPeriod();
		return (scalingFactor[hour] - 1) * (a * quantity * quantity + b * quantity);
	}

	@Override
	public double revenue() {
		int hour = ((int) RepastEssentials.GetTickCount()) % SmartGridBuilder.getPeriod();
		return a * scalingFactor[hour] * quantity * quantity + b * scalingFactor[hour] * quantity + c;
	}

	@Override
	public double getA() {
		// TODO Auto-generated method stub
		return a;
	}

	@Override
	public double getB() {
		// TODO Auto-generated method stub
		return b;
	}

	@Override
	public double getC() {
		// TODO Auto-generated method stub
		return c;
	}
	
	@Override
	public String getId() {
		return id;
	}
}