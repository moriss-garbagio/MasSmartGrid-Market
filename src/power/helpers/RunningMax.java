package power.helpers;

import java.util.Comparator;
import java.util.PriorityQueue;

import power.SmartGridBuilder;
import repast.simphony.essentials.RepastEssentials;

public class RunningMax extends RunningMean {

	protected final PriorityQueue<Double> recentMaxHeap;

	public RunningMax() {
		super();
		recentMaxHeap = new PriorityQueue<Double>(SmartGridBuilder.getWindowSize() + 1, new Comparator<Double>() {
			@Override
			public int compare(Double o1, Double o2) {
				if (o1 > o2) {
					return -1;
				} else if (o1 < o2) {
					return 1;
				} else {
					return 0;
				}
			}
		});
	}

	@Override
	public void add(double value) {
		period = (int) RepastEssentials.GetTickCount() % SmartGridBuilder.getPeriod();

		window.add(value);
		recentMaxHeap.add(value);

		periodicSumList[period] += value;

		if (window.size() > SmartGridBuilder.getPeriod()) {
			double leaving = window.get(window.size() - SmartGridBuilder.getPeriod() - 1);
			recentSum += value - leaving;
			recentMaxHeap.remove(leaving);

		} else {
			recentSum += value;
		}

		windowSum += value;

		while (window.size() > SmartGridBuilder.getWindowSize()) {
			value = window.remove();
			if (window.size() < SmartGridBuilder.getPeriod()) {
				recentSum -= value;
				recentMaxHeap.remove(value);
			}

			periodicSumList[(SmartGridBuilder.getPeriod() + (period - window.size()) % SmartGridBuilder.getPeriod()) % SmartGridBuilder.getPeriod()] -= value;
			windowSum -= value;
		}
	}

	public double getRecentMax() {
		if (recentMaxHeap.isEmpty()) {
			return 0;
		} else {
			return recentMaxHeap.peek();
		}
	}
}
