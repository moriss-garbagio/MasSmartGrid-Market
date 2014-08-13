package power.models;

import power.models.cores.IModel;
import power.tools.IDescribable;

/**
 * @author PowerAgents
 *
 */
public interface IRandomModel extends IDescribable {
	public double getMeanSeedValue();
	public double getCurrentValue();
	
	public IModel getModel();
	public double getCostFactor();
}
