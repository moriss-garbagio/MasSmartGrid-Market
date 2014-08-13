package power.helpers;

import java.util.Iterator;
import java.util.LinkedList;

import power.SmartGridBuilder;
import power.tools.IAdjuster;

public class Computations {
	/*
	 * Helper Methods
	 */
	public static double getMax(double[] numbers) {
		double maxValue = numbers[0];
		for (int i = 1; i < numbers.length; i++) {
			if (numbers[i] > maxValue) {
				maxValue = numbers[i];
			}
		}
		return maxValue;
	}

	public static double getMin(double[] numbers) {
		double minValue = numbers[0];
		for (int i = 1; i < numbers.length; i++) {
			if (numbers[i] < minValue) {
				minValue = numbers[i];
			}
		}
		return minValue;
	}

	public static double getMax(LinkedList<Double> list, int size) {
		double max = Double.NEGATIVE_INFINITY;
		int index = 0;
		Iterator<Double> iterator = list.descendingIterator();
		while (iterator.hasNext() && index < size) {
			double value = iterator.next();
			if (max < value) {
				max = value;
			}
			index++;
		}

		return max;
	}

	public static double getMax(LinkedList<Double> list, int size, IAdjuster adjuster) {
		double max = Double.NEGATIVE_INFINITY;
		int index = 0;
		Iterator<Double> iterator = list.descendingIterator();
		while (iterator.hasNext() && index < size) {
			double value = adjuster.adjust(iterator.next());
			if (max < value) {
				max = value;
			}
			index++;
		}

		return max;
	}

	public static double getMin(LinkedList<Double> list, int size) {
		double min = Double.POSITIVE_INFINITY;
		int index = 0;
		Iterator<Double> iterator = list.descendingIterator();
		while (iterator.hasNext() && index < size) {
			double value = iterator.next();
			if (min > value) {
				min = value;
			}
			index++;
		}

		return min;
	}

	public static double getMin(LinkedList<Double> list, int size, IAdjuster adjuster) {
		double min = Double.POSITIVE_INFINITY;
		int index = 0;
		Iterator<Double> iterator = list.descendingIterator();
		while (iterator.hasNext() && index < size) {
			double value = adjuster.adjust(iterator.next());
			if (min > value) {
				min = value;
			}
			index++;
		}

		return min;
	}

	public static double getMean(LinkedList<Double> list, int size) {
		double sum = 0.0;
		int index = 0;
		Iterator<Double> iterator = list.descendingIterator();
		while (iterator.hasNext() && index < size) {
			sum += iterator.next();
			index++;
		}

		return sum / (double) index;
	}

	public static double getMean(LinkedList<Double> list, int size, IAdjuster adjuster) {
		double sum = 0.0;
		int index = 0;
		Iterator<Double> iterator = list.descendingIterator();
		while (iterator.hasNext() && index < size) {
			sum += adjuster.adjust(iterator.next());
			index++;
		}

		return sum / (double) index;
	}

	public static double getStandardDev(LinkedList<Double> list, int size) {
		if (list.size() > 1 && size > 1) {
			double sum = 0;
			int index = 0;
			double mean = getMean(list, size);
			Iterator<Double> iterator = list.descendingIterator();
			while (iterator.hasNext() && index < size) {
				sum += Math.pow(iterator.next() - mean, 2);
				index++;
			}

			return Math.sqrt(sum / (double) (index - 1));
		} else {
			return 0;
		}
	}

	public static double getStandardDev(LinkedList<Double> list, int size, IAdjuster adjuster) {
		if (list.size() > 1 && size > 1) {
			double sum = 0;
			int index = 0;
			double mean = getMean(list, size, adjuster);
			Iterator<Double> iterator = list.descendingIterator();
			while (iterator.hasNext() && index < size) {
				sum += Math.pow(adjuster.adjust(iterator.next()) - mean, 2);
				index++;
			}

			return Math.sqrt(sum / (double) (index - 1));
		} else {
			return 0;
		}
	}

	public static double getPredictedValueByLinearAvg(LinkedList<Double> list, int foresight) {
		foresight %= SmartGridBuilder.getPeriod();
		int numberOfSamples = (list.size() + foresight) / SmartGridBuilder.getPeriod();
		if (numberOfSamples > 0) {
			double demand = 0.0;
			int index = foresight;

			int weight = numberOfSamples;
			double someOfWeights = 0;

			Iterator<Double> iterator = list.descendingIterator();

			while (iterator.hasNext()) {
				double value = iterator.next();
				if (index++ % SmartGridBuilder.getPeriod() == 0) {
					demand += value * weight;
					someOfWeights += weight--;
				}
			}

			return demand / someOfWeights;
		} else {
			return 0;
		}
	}

	public static double getPredictedValueByStudy(LinkedList<Double> list, int foresight) {
		foresight %= SmartGridBuilder.getPeriod();
		if (list.size() + foresight >= SmartGridBuilder.getPeriod()) {
			// there is at least one sample
			double dataSum = 0.0;
			double weightSum = 0.0;
			int dataIndex = foresight;
			int weightIndex = 0;

			double weights[] = { 0.182213, 0.150759, 0.136659, 0.127983, 0.124729, 0.133406, 0.144252 };

			Iterator<Double> iterator = list.descendingIterator();

			while (iterator.hasNext() && weightIndex < weights.length) {
				double powerDemand = iterator.next();
				if (dataIndex++ % SmartGridBuilder.getPeriod() == 0) {
					dataSum += powerDemand * weights[weightIndex];
					weightSum += weights[weightIndex++];
				}
			}
			return dataSum / weightSum;
		} else {
			return 0;
		}
	}

	public static double getExponentialAverage(Double oldValue, double newValue) {
		if (oldValue == null) {
			return newValue;
		} else {
			return oldValue * SmartGridBuilder.getHistoryValue() + newValue * (1.0 - SmartGridBuilder.getHistoryValue());
		}
	}

	public static double getMean(double[] list) {
		double sum = 0;
		for (double item : list) {
			sum += item;
		}
		return sum / list.length;
	}
}
