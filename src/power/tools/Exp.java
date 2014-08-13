package power.tools;

public class Exp implements IAdjuster {
	private static Exp singleton = new Exp();

	public static Exp getSingleton() {
		return singleton;
	}

	private Exp() {
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
		String str = "Exponential Adjustment\n";
		return str;
	}

	@Override
	public double adjust(double value) {
		return Math.exp(value);
	}

	@Override
	public int adjust(int value) {
		return (int) Math.exp(value);
	}
}
