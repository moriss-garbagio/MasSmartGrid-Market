package power.distributions;

import power.tools.IDescribable;

public interface IRandomDistribution extends IDescribable {
	public double nextDouble();
	public int nextInt();
}
