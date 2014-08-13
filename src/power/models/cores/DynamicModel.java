package power.models.cores;

import java.util.Arrays;

import power.tools.IAdjuster;
import power.tools.IDescribable;

public abstract class DynamicModel extends Model {
	
	protected static class Property implements IDescribable {
		private final String property;
		private final IAdjuster adjuster;
		
		public Property(String property, IAdjuster adjuster) {
			this.property = property;
			this.adjuster = adjuster;
		}

		public String getProperty() {
			return property;
		}

		public IAdjuster getAdjuster() {
			return adjuster;
		}

		@Override
		public String description() {
			return this.description(0);
		}

		@Override
		public String description(int nestingLevel) {
			String tabbing = "";
			if (nestingLevel > 0) tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
			String str = "Property: {\n\t" +
					tabbing + "property: " + property + "\n\t" +
					tabbing + "adjuster: " + adjuster.description(nestingLevel + 1) +
				tabbing + "}\n";
			return str;
		}
	}
	
	private final Property[] propertyList;
	
	public DynamicModel(Property[] propertyList) {
		this.propertyList = propertyList;
		source = null;
	}
	
	protected double getPropertyValue(int propertyIndex) {
		try {
			if (source != null && propertyList != null && propertyIndex < propertyList.length) {
				if (propertyList[propertyIndex].getAdjuster() != null) {
					return propertyList[propertyIndex].getAdjuster().adjust((Double) source.getClass().getMethod(propertyList[propertyIndex].getProperty()).invoke(source));
				} else {
					return (Double) source.getClass().getMethod(propertyList[propertyIndex].getProperty()).invoke(source);
				}
			} else {
				System.err.println("ExponentialFunctionModel: has not been initialized propertly.");
				System.err.println("Source: " + source + ", propertyList: " + Arrays.toString(propertyList));
				return Double.NaN;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Double.NaN;
	}
	
	@Override
	public String description(int nestingLevel) {
		String tabbing = "";
		if (nestingLevel > 0) tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
		String str = "DynamicFunctionModel: {\n\t" +
				tabbing + "source: " + getSource().toString() + "\n\t" + 
				tabbing + "popertyList: [ \n\t\t"; 
		
		for (Property property : propertyList) {
			str += tabbing + property.description(nestingLevel + 2);
		}
		str += tabbing + "\t]\n" +
				tabbing + "}\n";
		return str;
	}
}
