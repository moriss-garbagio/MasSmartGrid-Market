package power.components;

public class BlankReliability implements IReliability {
	private static BlankReliability singleton = new BlankReliability();

	public static IReliability getBlankReliability() {
		return singleton;
	}

	@Override
	public boolean isOperational() {
		return true;
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
		String str = "BlankReliability: { }\n";
		return str;
	}
}
