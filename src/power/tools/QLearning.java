package power.tools;
import power.helpers.Computations;

public class QLearning {
	private double qTable[][];
	private final double alpha;
	private final double gamma;

	public QLearning(int states, int actions, double alpha, double gamma) {
		qTable = new double[states][actions];
		this.alpha = alpha;
		this.gamma = gamma;
	}

	public void learn(int previousState, int previousAction, int currentState, double reward) {
		// update qTable
		for (int state = 0; state < qTable.length; state++) {
			for (int action = 0; action < qTable[state].length; action++) {
				qTable[previousState][previousAction] = (1 - alpha) * qTable[previousState][previousAction] + gamma * reward * Computations.getMax(qTable[currentState]);
			}
		}
	}
	
	public double getQValue(int state, int action) {
		return qTable[state][action];
	}
}
