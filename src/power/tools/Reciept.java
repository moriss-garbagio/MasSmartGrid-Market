package power.tools;

public class Reciept {
	private double requestedAmount;
	private double satisfiedAmount;
	
	public Reciept(double requestedAmount, double satisfiedAmount) {
		this.requestedAmount = requestedAmount;
		this.satisfiedAmount = satisfiedAmount;
	}
	
	public double getRequestedAmount() {
		return requestedAmount;
	}
	
	public double getSatisfiedAmount() {
		return satisfiedAmount;
	}
	
	public double getRemainingAmount() {
		return requestedAmount - satisfiedAmount;
	}
}
