package power.helpers;

import power.SmartGridBuilder;
import power.tools.CycleQueue;
import power.tools.IAdjuster;
import repast.simphony.essentials.RepastEssentials;

public class RunningMean {
	protected final double[] periodicSumList;
	protected double recentSum;
	protected double windowSum;

	protected final CycleQueue<Double> window;

	protected int period;

	public RunningMean() {
		window = new CycleQueue<Double>(SmartGridBuilder.getWindowSize() + 1);
		periodicSumList = new double[(int) SmartGridBuilder.getPeriod()];
		clearCache();
	}

	public void add(double value) {
		period = (int) RepastEssentials.GetTickCount() % SmartGridBuilder.getPeriod();

		window.add(value);

		periodicSumList[period] += value;

		if (window.size() > SmartGridBuilder.getPeriod()) {
			recentSum += value - window.get(window.size() - SmartGridBuilder.getPeriod() - 1);
		} else {
			recentSum += value;
		}

		windowSum += value;

		while (window.size() > SmartGridBuilder.getWindowSize()) {
			value = window.remove();
			if (window.size() < SmartGridBuilder.getPeriod()) {
				recentSum -= value;
			}

			periodicSumList[(SmartGridBuilder.getPeriod() + (period - window.size()) % SmartGridBuilder.getPeriod()) % SmartGridBuilder.getPeriod()] -= value;
			windowSum -= value;
		}
	}

	public double getPeriodMean(int foresight) {
		foresight = foresight % SmartGridBuilder.getPeriod();
		int numberOfValues = (window.size() + (foresight - SmartGridBuilder.getPeriod()) % SmartGridBuilder.getPeriod() - 1) / SmartGridBuilder.getPeriod() + 1;
		int index = (period + foresight) % SmartGridBuilder.getPeriod();
		return periodicSumList[index] / numberOfValues;
	}

	public double getRecentSum() {
		return recentSum;
	}

	public double getRecentMean() {
		if (window.size() > 0) {
			if (window.size() < SmartGridBuilder.getPeriod()) {
				return recentSum / window.size();
			} else {
				return recentSum / SmartGridBuilder.getPeriod();
			}
		} else {
			return 0;
		}
	}

	public double getWindowSum() {
		return windowSum;
	}

	public double getWindowMean() {
		return windowSum / window.size();
	}

	public double getMean(int size) {
		if (size == SmartGridBuilder.getPeriod() || (window.size() < SmartGridBuilder.getPeriod() && size == window.size())) {
			return getRecentMean();
		} else if (size == SmartGridBuilder.getWindowSize() || (window.size() < SmartGridBuilder.getWindowSize() && size == window.size())) {
			return getWindowMean();
		} else {
			return recomputeSum(size) / Math.min(size, window.size());
		}
	}

	public double recomputeVariance(int size) {
		if (window.size() > 0 && size > 0) {
			double sum = 0;
			double mean = getMean(size);

			for (int index = window.size() > size ? window.size() - size : 0; index < window.size(); index++) {
				sum += Math.pow(window.get(index) - mean, 2);
			}
			return sum;
		} else {
			return 0;
		}
	}
	
	public double recomputeStandardDev(int size) {
		if (window.size() > 0 && size > 0) {
			return Math.sqrt(recomputeVariance(size) / (Math.min(size, window.size()) - 1));
		} else {
			return 0;
		}
	}

	public double recomputeStandardDev(int size, IAdjuster adjuster) {
		if (window.size() > 0 && size > 0) {
			double sum = 0;
			double mean = adjuster.adjust(getMean(size));

			for (int index = window.size() > size ? window.size() - size : 0; index < window.size(); index++) {
				sum += Math.pow(adjuster.adjust(window.get(index)) - mean, 2);
			}
			return Math.sqrt(sum / (Math.min(size, window.size()) - 1));
		} else {
			return 0;
		}
	}

	protected double recomputePeriodicSum(int foresight) {
		double sum = 0;
		for (int index = window.size() + (foresight - SmartGridBuilder.getPeriod()) % SmartGridBuilder.getPeriod() - 1; index >= 0; index -= (int) SmartGridBuilder.getPeriod()) {
			sum += window.get(index);
		}
		return sum;
	}

	protected double recomputeSum(int size) {
		double sum = 0;
		for (int index = window.size() > size ? window.size() - size : 0; index < window.size(); index++) {
			sum += window.get(index);
		}
		return sum;
	}

	protected void clearCache() {
		recentSum = 0;
		windowSum = 0;
		for (int index = 0; index < periodicSumList.length; index++) {
			periodicSumList[index] = 0;
		}
	}

	public CycleQueue<Double> getWindow() {
		return window;
	}

	public int getWindowSize() {
		return window.size();
	}

	public int getRecentSize() {
		if (window.size() < SmartGridBuilder.getPeriod()) {
			return window.size();
		} else {
			return SmartGridBuilder.getPeriod();
		}
	}
}
