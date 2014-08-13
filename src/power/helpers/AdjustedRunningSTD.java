package power.helpers;

import power.SmartGridBuilder;
import power.tools.Adjuster;
import repast.simphony.essentials.RepastEssentials;

public class AdjustedRunningSTD extends AdjustedRunningMax {
	protected double recentSquaredSum;
	protected double windowSquaredSum;

	public AdjustedRunningSTD(Adjuster maxAdjuster) {
		super(maxAdjuster);
	}

	@Override
	public void add(double value) {
		period = (int) RepastEssentials.GetTickCount() % SmartGridBuilder.getPeriod();

		window.add(value);
		recentMaxHeap.add(maxAdjuster.adjust(value));

		periodicSumList[period] += value;

		if (window.size() > SmartGridBuilder.getPeriod()) {
			double leaving = window.get(window.size() - SmartGridBuilder.getPeriod() - 1);
			recentSum += value - leaving;
			recentSquaredSum += value * value - leaving * leaving;
			recentMaxHeap.remove(maxAdjuster.adjust(leaving));
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
				recentMaxHeap.remove(maxAdjuster.adjust(value));
			}

			periodicSumList[(window.size() + period) % SmartGridBuilder.getPeriod()] -= value;
			windowSum -= value;
			windowSquaredSum -= value * value;
		}
	}
	public double getRecentVatiance() {
		int sampeCount = getRecentSize();
		if (sampeCount > 1) {
			double recentSum = getRecentSum();
			return recentSquaredSum - recentSum * recentSum / sampeCount;
		} else {
			return 0;
		}	
	}

	public double getWindowVariance() {
		int sampeCount = getWindowSize();
		if (sampeCount > 1) {
			double windowSum = getWindowSum();
			return windowSquaredSum - windowSum * windowSum / sampeCount;
		} else {
			return 0;
		}
	}
	
	public double getRecentSTD() {
		int sampeCount = getRecentSize();
		if (sampeCount > 1) {
			return Math.sqrt(getRecentVatiance() / (sampeCount - 1));
		} else {
			return 0;
		}
	}

	public double getWindowSTD() {
		int sampeCount = getWindowSize();
		if (sampeCount > 1) {
			return Math.sqrt(getWindowVariance() / (sampeCount - 1));
		} else {
			return 0;
		}
	}
}
