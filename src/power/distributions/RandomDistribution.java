package power.distributions;

import java.util.ArrayList;

import org.w3c.dom.Node;

import power.helpers.XmlTools;

import power.tools.Adjuster;
import power.tools.IAdjuster;
import power.tools.IDescribable;
import repast.simphony.random.RandomHelper;
import cern.jet.random.AbstractDistribution;

public class RandomDistribution implements IRandomDistribution, IDescribable {
	private enum XmlNode { Adjuster, Alpha, Beta, N, M, P, S, Mean, Gamma, Cut , Freedom , Pdf, InterpolationType, Lambda, Tau , StandardDeviation, Min, Max, Ro, Pk }
	private enum RandomDistributionType {
		Uniform, // default
		Beta,
		Binomial,
		BreitWigner,
		BreitWignerMeanSquare,
		ChiSquare,
		Empirical,
		EmpiricalWalker,
		Exponential,
		ExponentialPower,
		Gamma,
		Hyperbolic,
		HyperGeometric,
		Logarithmic,
		NegativeBinomial,
		Normal,
		Poisson,
		PoissonSlow,
		StudentT,
		VonMises,
		Zeta,
		
		// others
		Constant,
		LogNormal
	}
	
	private final AbstractDistribution abstractDistribution;
	private final IAdjuster adjuster;
	
	public static IRandomDistribution create(Node xml) {
		if (xml == null) return null;
		
		RandomDistributionType type = XmlTools.getAttributeValue(xml, XmlTools.XmlAttribute.type, RandomDistributionType.class);
		IAdjuster adjuster = Adjuster.createAll(XmlTools.getAllNodes(xml, XmlNode.Adjuster));
		switch (type) {
		case Beta: {
			double alpha = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Alpha), XmlTools.XmlAttribute.value));
			double beta = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Beta), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createBeta(alpha, beta), adjuster);
		}
		case Binomial: {
			int n = Integer.parseInt(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.N), XmlTools.XmlAttribute.value));
			double p = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.P), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createBinomial(n, p), adjuster);
		}
		case BreitWigner: {
			double mean = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Mean), XmlTools.XmlAttribute.value));
			double gamma = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Gamma), XmlTools.XmlAttribute.value));
			double cut = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Cut), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createBreitWigner(mean, gamma, cut), adjuster);
		}
		case BreitWignerMeanSquare: {
			double mean = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Mean), XmlTools.XmlAttribute.value));
			double gamma = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Gamma), XmlTools.XmlAttribute.value));
			double cut = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Cut), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createBreitWignerMeanSquare(mean, gamma, cut), adjuster);
		}
		case ChiSquare: {
			double freedom = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Freedom), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createChiSquare(freedom), adjuster);
		}
		case Empirical: {
			ArrayList<Node> nodeList = XmlTools.getAllNodes(xml, XmlNode.Pdf);
			double[] pdf = new double[nodeList.size()];
			for (int index = 0; index < pdf.length; index++) {
				pdf[index] = Double.parseDouble(XmlTools.getAttributeValue(nodeList.get(index), XmlTools.XmlAttribute.value));
			}
			int interpolationType = Integer.parseInt(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.InterpolationType), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createEmpirical(pdf, interpolationType));
		}
		case EmpiricalWalker: {
			ArrayList<Node> nodeList = XmlTools.getAllNodes(xml, XmlNode.Pdf);
			double[] pdf = new double[nodeList.size()];
			for (int index = 0; index < pdf.length; index++) {
				pdf[index] = Double.parseDouble(XmlTools.getAttributeValue(nodeList.get(index), XmlTools.XmlAttribute.value));
			}
			int interpolationType = Integer.parseInt(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.InterpolationType), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createEmpiricalWalker(pdf, interpolationType), adjuster);
		}
		case Exponential: {
			double lambda = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Lambda), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createExponential(lambda), adjuster);
		}
		case ExponentialPower: {
			double tau = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Tau), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createExponentialPower(tau), adjuster);
		}
		case Gamma: {
			double alpha = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Alpha), XmlTools.XmlAttribute.value));
			double lambda = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Lambda), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createGamma(alpha, lambda), adjuster);
		}
		case Hyperbolic: {
			double alpha = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Alpha), XmlTools.XmlAttribute.value));
			double beta = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Beta), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createHyperbolic(alpha, beta), adjuster);
		}
		case HyperGeometric: {
			int N = Integer.parseInt(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.N), XmlTools.XmlAttribute.value));
			int s = Integer.parseInt(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.S), XmlTools.XmlAttribute.value));
			int n = Integer.parseInt(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.M), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createHyperGeometric(N, s, n), adjuster);
		}
		case Logarithmic: {
			double p = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.P), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createLogarithmic(p), adjuster);
		}
		case NegativeBinomial: {
			int n = Integer.parseInt(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.N), XmlTools.XmlAttribute.value));
			double p = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.P), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createNegativeBinomial(n, p), adjuster);
		}
		case Normal: {
			double mean = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Mean), XmlTools.XmlAttribute.value));
			double standardDeviation = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.StandardDeviation), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createNormal(mean, standardDeviation), adjuster);
		}
		case Poisson: {
			double mean = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Mean), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createPoisson(mean), adjuster);
		}
		case PoissonSlow: {
			double mean = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Mean), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createPoissonSlow(mean), adjuster);
		}
		case StudentT: {
			double freedom = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Freedom), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createStudentT(freedom), adjuster);
		}
		case VonMises: {
			double freedom = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Freedom), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createVonMises(freedom), adjuster);
		}
		case Zeta: {
			double ro = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Ro), XmlTools.XmlAttribute.value));
			double pk = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Pk), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createZeta(ro, pk), adjuster);
		}
		case Constant: {
			return ConstantDistribution.create(xml);
		}
		case LogNormal: {
			return LogNormalDistribution.create(xml);
		}
		case Uniform:
		default: {
			double min = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Min), XmlTools.XmlAttribute.value));
			double max = Double.parseDouble(XmlTools.getAttributeValue(XmlTools.getExactlyOneNode(xml, XmlNode.Max), XmlTools.XmlAttribute.value));
			return new RandomDistribution(RandomHelper.createUniform(min, max), adjuster); 
		} }
	}
	
	public RandomDistribution(AbstractDistribution abstractDistribution) {
		this(abstractDistribution, null);
	}
	
	public RandomDistribution(AbstractDistribution abstractDistribution, IAdjuster adjuster) {
		this.abstractDistribution = abstractDistribution;
		this.adjuster = adjuster;
	}
	
	@Override
	public double nextDouble() {
		if (adjuster != null) {
			return adjuster.adjust(abstractDistribution.nextDouble());
		} else {
			return abstractDistribution.nextDouble();
		}
	}

	@Override
	public int nextInt() {
		if (adjuster != null) {
			return adjuster.adjust(abstractDistribution.nextInt());
		} else {
			return abstractDistribution.nextInt();
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
		String str = "RandomDistribution: {\n\t" +
				tabbing + "abstractDistribution: " + abstractDistribution + "\n\t" +
				tabbing + "adjuster: " + (adjuster == null ? "null\n" : adjuster.description(nestingLevel+1)) +
				tabbing + "}\n";
		return str;
	}
}
