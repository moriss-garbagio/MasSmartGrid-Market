package power.auction;

public class Bid {
	public double a, b;
	public double q_min, q_max;

	public Bid(double a, double b, double q_min, double q_max) {
		this.a = a;
		this.b = b;
		this.q_max = q_max;
		this.q_min = q_min;
	}
}