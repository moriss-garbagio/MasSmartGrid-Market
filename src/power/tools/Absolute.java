package power.tools;

public class Absolute implements IAdjuster {
	private static Absolute singleton = new Absolute();

	public static Absolute getSingleton() {
		return singleton;
	}

	private Absolute() {
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
		String str = "Absolute Adjustment\n";
		return str;
	}

	@Override
	public double adjust(double value) {
		return Math.abs(value);
	}

	@Override
	public int adjust(int value) {
		return Math.abs(value);
	}
}
