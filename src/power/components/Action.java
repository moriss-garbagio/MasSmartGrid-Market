package power.components;

public class Action<T> implements Comparable<Action<T>>{
	final T id;
	double utility;
	
	public Action(T actionId, double actionUtility) {
		this.id = actionId;
		this.utility = actionUtility;
	}
	
	public double getUtility() {
		return utility;
	}

	public void setUtility(double utility) {
		this.utility = utility;
	}

	public T getId() {
		return id;
	}

	@Override
	public int compareTo(Action<T> action) {
		return Double.compare(this.getUtility(), action.getUtility());
	}
}
