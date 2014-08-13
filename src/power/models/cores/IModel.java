package power.models.cores;

import power.tools.IDescribable;

public interface IModel extends IDescribable{
	
	public double getMeanSeedValue();
	/**
	 * @return a value from [0, 1] indicating the average value for the random function  
	 */
	public double getValue();
	public void setSource(Object source);
	public Object getSource();
}
