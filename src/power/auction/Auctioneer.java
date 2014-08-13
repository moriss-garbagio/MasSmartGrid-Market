package power.auction;

import java.util.ArrayList;

import org.w3c.dom.Node;

import power.components.Grid;
import power.components.IReliability;
import power.components.IPowerPlant;
import power.components.PowerPlant;
import power.components.Reliability;
import power.distributions.IRandomDistribution;
import power.distributions.RandomDistribution;
import power.helpers.XmlTools;
import repast.simphony.engine.schedule.ScheduledMethod;

public class Auctioneer extends Grid {

	private enum XmlNode {
		PowerRateRandomModel,
		PriceAdjustment,
		BlackoutRadius,
		PowerPlant,
		Accuracy,
		Epsilon,
		Lambda,
		ScaleUpFactor,
		StopLock,
		ScaleDownFactor
	};

	private ArrayList<PowerPlant> powerPlantList;
	private final double accuracy;
	private final double epsilon;
	private final double lambda;
	private final double scaleUpFactor;
	private final double scaleDownFactor;
	private final int stopLock;

	public static Grid create(Node xml) {
		IReliability reliability = Reliability.create(xml);
		IRandomDistribution blackoutRadius = RandomDistribution.create(XmlTools.getUptoOneNode(xml, XmlNode.BlackoutRadius));

		double priceAdjustment = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.PriceAdjustment), XmlTools.XmlAttribute.value));

		ArrayList<Node> nodeList = XmlTools.getAtLeastOneNode(xml, XmlNode.PowerPlant);
		ArrayList<PowerPlant> generatorList = new ArrayList<PowerPlant>();
		for (Node node : nodeList) {
			generatorList.add(PowerPlant.create(node));
		}

		String value = null;

		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.Accuracy), XmlTools.XmlAttribute.value);
		double accuracy = value != null ? Double.parseDouble(value) : 0.1;

		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.Epsilon), XmlTools.XmlAttribute.value);
		double epsilon = value != null ? Double.parseDouble(value) : 10;

		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.Lambda), XmlTools.XmlAttribute.value);
		double lambda = value != null ? Double.parseDouble(value) : 10;

		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.ScaleUpFactor), XmlTools.XmlAttribute.value);
		double scaleUpFactor = value != null ? Double.parseDouble(value) : 1.1;

		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.ScaleDownFactor), XmlTools.XmlAttribute.value);
		double scaleDownFactor = value != null ? Double.parseDouble(value) : 0.9;
		
		value = XmlTools.getAttributeValue(XmlTools.getUptoOneNode(xml, XmlNode.StopLock), XmlTools.XmlAttribute.value);
		int stopLock = value != null ? Integer.parseInt(value) : 5000;

		return new Auctioneer(blackoutRadius, reliability, priceAdjustment, accuracy, epsilon, lambda, scaleUpFactor, scaleDownFactor, stopLock, generatorList);
	}

	private Auctioneer(IRandomDistribution blackoutRadius,
			IReliability reliability,
			double priceAdjustment,
			double accuracy,
			double epsilon,
			double lambda,
			double scaleUpFactor,
			double scaleDownFactor,
			int stopLock,
			ArrayList<PowerPlant> generatorList) {
		super(blackoutRadius, reliability, priceAdjustment);
		this.powerPlantList = generatorList;
		this.accuracy = accuracy;
		this.epsilon = epsilon;
		this.lambda = lambda;
		this.scaleUpFactor = scaleUpFactor;
		this.scaleDownFactor = scaleDownFactor;
		this.stopLock = stopLock;
	}

	public void initialize() {
		super.initialize();
		currentBasePrice = null;
	}

	private Double currentBasePrice = null;

	public double getCurrentBasePrice() {
		if (currentBasePrice == null) {
			return getPredictedBasePrice();
		} else {
			return currentBasePrice;
		}
	}

	@ScheduledMethod(start = 0, interval = 1, priority = -2)
	public void execute() {
		double epsilon = this.epsilon;
		double lambda = this.lambda;
		double quota[] = new double[powerPlantList.size()];
		// System.err.println("-" + getCurrentDemand() + ", " + currentSoldPower
		// + ", " + currentBoughtPower + ", gens" + powerPlantList.size());

		double renewableQuantity = 0;
		double weightedAvgRenuableCost = 0;
		double minConventionalQuantity = 0;

		for (int i = 0; i < powerPlantList.size(); i++) {
			Bid bid = powerPlantList.get(i).getBid();
			if (bid.a == 0 && bid.b == 0) {
				renewableQuantity += bid.q_max;
				weightedAvgRenuableCost += powerPlantList.get(i).getC() * bid.q_max;
			} else {
				minConventionalQuantity += bid.q_min;
			}
		}
		
		if (renewableQuantity > 0) {
			weightedAvgRenuableCost /= renewableQuantity;
		} else { 
			weightedAvgRenuableCost = 0;
		}
		
		double currentDemand = Math.max(getCurrentDemand() - renewableQuantity, minConventionalQuantity);
				
		int stopLock = this.stopLock;
		while (Math.abs(epsilon) > this.accuracy && stopLock-- > 0) {
			if (epsilon > 0) {
				lambda *= this.scaleUpFactor;
			} else {
				lambda *= this.scaleDownFactor;
			}
			double quotaTotal = 0;
			for (int i = 0; i < powerPlantList.size(); i++) {
				Bid bid = powerPlantList.get(i).getBid();
				if (bid.a != 0 && bid.b != 0) {
					quota[i] = (lambda - bid.b) / (2 * bid.a);
					// System.err.println(":" + i + ", " + quota[i] + ", bid = "
					// +
					// bid.a + ", " + bid.b);
					if (quota[i] < bid.q_min) {
						quota[i] = bid.q_min;
					} else if (quota[i] > bid.q_max) {
						quota[i] = bid.q_max;
					}
					quotaTotal += quota[i];
				}
			}
			epsilon = currentDemand - quotaTotal;
		}
		
		if(stopLock <= 0) {
			if(epsilon > 0) {
				System.out.println("Not enough power.");
			} else {
				System.out.println("Too much power.");
			}
		}

		for (int i = 0; i < powerPlantList.size(); i++) {
			powerPlantList.get(i).execute(quota[i]);
		}

//		currentBasePrice = Math.min(lambda, weightedAvgRenuableCost);
		currentBasePrice = lambda;
	}
	
	@Override
	public ArrayList<PowerPlant> getPlantList() {
		// TODO Auto-generated method stub
		return powerPlantList;
	}
}
