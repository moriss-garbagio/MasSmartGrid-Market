package power.helpers;

import power.SmartGridBuilder;
import power.tools.Adjuster;
import repast.simphony.essentials.RepastEssentials;

public class RunningSTD extends RunningMax {
	protected double recentSquaredSum;
	protected double windowSquaredSum;

	@Override
	public void add(double value) {
		period = (int) RepastEssentials.GetTickCount() % SmartGridBuilder.getPeriod();

		window.add(value);
		recentMaxHeap.add(value);

		periodicSumList[period] += value;

		if (window.size() > SmartGridBuilder.getPeriod()) {
			double leaving = window.get(window.size() - SmartGridBuilder.getPeriod() - 1);
			recentSum += value - leaving;
			recentSquaredSum += value * value - leaving * leaving;
			recentMaxHeap.remove(window.get(window.size() - SmartGridBuilder.getPeriod() - 1));
		} else {
			recentSum += value;
			recentSquaredSum += value * value;
		}

		windowSum += value;
		windowSquaredSum += value * value;

		while (window.size() > SmartGridBuilder.getWindowSize()) {
			value = window.remove();
			if (window.size() < SmartGridBuilder.getPeriod()) {
				recentSum -= value;
				recentSquaredSum -= value * value;
				recentMaxHeap.remove(value);
			}

			periodicSumList[(SmartGridBuilder.getPeriod() + (period - window.size()) % SmartGridBuilder.getPeriod()) % SmartGridBuilder.getPeriod()] -= value;
			windowSum -= value;
			windowSquaredSum -= value * value;
		}
	}

	public double getRecentSTD() {
		int sampeCount = getRecentSize();
		if (sampeCount > 1) {
			double recentSum = getRecentSum();
			return (recentSquaredSum - recentSum * recentSum / sampeCount) / (sampeCount - 1);
		} else {
			return 0;
		}
	}

	public double getWindowSTD() {
		int sampeCount = getWindowSize();
		if (sampeCount > 1) {
			double windowSum = getWindowSum();
			return (windowSquaredSum - windowSum * windowSum / sampeCount) / (sampeCount - 1);
		} else {
			return 0;
		}
	}
}
