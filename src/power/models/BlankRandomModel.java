package power.models;

import power.models.cores.IModel;
import power.tools.IDescribable;

public class BlankRandomModel implements IRandomModel, IDescribable {
	
	private static IRandomModel singleton = new BlankRandomModel();
	
	public static IRandomModel getBlankRandomModel() {
		return singleton;
	}
	
	private BlankRandomModel() { }
	
	@Override
	public double getMeanSeedValue() {
		return Double.NaN;
	}
	
	@Override
	public double getCurrentValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IModel getModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getCostFactor() {
		// TODO Auto-generated method stub
		return 0;
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
		String str = "BlankRandomModel: { Note: Return 0 for all properties }\n";
		return str;
	}
}
