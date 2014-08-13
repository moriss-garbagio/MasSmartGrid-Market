package power.auction;

import org.w3c.dom.Node;

import power.components.Grid;
import power.components.IReliability;
import power.components.Reliability;
import power.distributions.IRandomDistribution;
import power.distributions.RandomDistribution;
import power.helpers.XmlTools;
import power.models.IRandomModel;
import power.models.RandomModel;

public class Market extends Grid {
	
	private enum XmlNode {
		PowerRateRandomModel,
		PriceAdjustment,
		BlackoutRadius,
	};	

	private final IRandomModel randomModel;

	public static Grid create(Node xml) {
		IReliability reliability = Reliability.create(xml);
		IRandomDistribution blackoutRadius = RandomDistribution.create(XmlTools.getUptoOneNode(xml, XmlNode.BlackoutRadius));

		IRandomModel randomModel = RandomModel.create(XmlTools.getExactlyOneNode(xml, XmlNode.PowerRateRandomModel));
		double priceAdjustment = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.PriceAdjustment), XmlTools.XmlAttribute.value));
		return new Market(randomModel, blackoutRadius, reliability, priceAdjustment);
	}

	public Market(IRandomModel randomModel,
			IRandomDistribution blackoutRadius,
			IReliability reliability,
			double priceAdjustment) {
		super(blackoutRadius, reliability, priceAdjustment);
		this.randomModel = randomModel;
		this.randomModel.getModel().setSource(this);
	}
	
	public double getCurrentBasePrice() {
		return randomModel.getCurrentValue();
	}
}