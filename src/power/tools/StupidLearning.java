package power.tools;

import cern.jet.random.Empirical;
import repast.simphony.random.RandomHelper;

public class StupidLearning {
	private double stupidTable[];
	private double probabilityList[];
	private double minValue;
	private double offset;
	private final double alpha;

	public StupidLearning(int actions, double minValue,  double alpha) {
		this.minValue = minValue;
		this.alpha = alpha;
		
		offset = 0.0;		
		stupidTable = new double[actions];
		for (int i = 0; i < stupidTable.length; i++) {
			stupidTable[i] = minValue;
		}
		probabilityList = new double[actions];
	}

	public void learn(int action, double reward) {
		stupidTable[action] = (1 - alpha) * stupidTable[action] + alpha * (reward + offset);
		
		if (stupidTable[action] < 0) { 
			double delta = minValue - stupidTable[action];
			for (int i = 0; i < stupidTable.length; i++) {
				stupidTable[i] += delta;
			}
			offset += delta;
		}
	}
	
	public double getProbability(int action) {
		double sum = 0;
		for (double stupid : stupidTable) {
			sum += stupid;
		}
		return stupidTable[action]/sum;
	}
	
	public int getNextAction() {
		double sum = 0;
		for (double stupid : stupidTable) {
			sum += stupid;
		}
		
		for (int i = 0; i < probabilityList.length; i++) {
			probabilityList[i] = stupidTable[i]/sum;
		}
		
		return (int) Math.round(RandomHelper.createEmpirical(probabilityList, Empirical.NO_INTERPOLATION).nextDouble() * stupidTable.length);
	}
	
}
