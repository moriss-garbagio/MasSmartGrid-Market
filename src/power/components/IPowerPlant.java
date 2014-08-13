package power.components;

import power.auction.Bid;

public interface IPowerPlant {
	public void execute(double quota);
	public Bid getBid();
	
	public double getQuantity();
	public double reward();
	public double revenue();
	
	public String getId();
	
	public double getA();
	public double getB();
	public double getC();
}
