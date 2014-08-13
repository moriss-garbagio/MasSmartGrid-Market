package power.networks;

import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.context.space.graph.NetworkGenerator;

public class MinMaxNetworkGenerator<T> implements NetworkGenerator<T> {
	
	private int minDegree;
	private int maxDegree;
	private int maxNumberOfRounds;
	private boolean isConnected;
	
	public MinMaxNetworkGenerator(int minDegree, int maxDegree, boolean isConnected) {
		initialize(minDegree, maxDegree, isConnected);
		this.maxNumberOfRounds = (int)Math.ceil((this.maxDegree - this.minDegree) / 2.0);
	}

	public MinMaxNetworkGenerator(int minDegree, int maxDegree, int maxNumberOfRounds, boolean isConnected) {
		initialize(minDegree, maxDegree, isConnected);
		this.maxNumberOfRounds = maxNumberOfRounds;
	}
	
	private void initialize(int minDegree, int maxDegree, boolean isConnected) {
		if (maxDegree < minDegree) {
			this.minDegree = maxDegree;
			this.maxDegree = minDegree;
		} else {
			this.minDegree = minDegree;
			this.maxDegree = maxDegree;
		}
		
		if (this.minDegree < 0) this.minDegree = 0;
		if (isConnected) {
			if (this.maxDegree < 2) this.maxDegree = 2;
		} else {
			if (this.maxDegree < 1) this.maxDegree = 1;
		}
		this.isConnected = isConnected;
	}
	
	@Override
	public Network<T> createNetwork(Network<T> network) {
		
		int numberOfNodes = 0, numberOfMaxDegree = 0, numberOfSufficientDegree = 0;
		if (isConnected) {
			T core = null;
			int counter = maxDegree;
			for (T current : network.getNodes()) {
				if(core != null) {
					network.addEdge(core, current);
				} else {
					core = current;
				}
				if (counter == 0) {
					counter = maxDegree - 1;
					core = current;
				}
				counter--;
				numberOfNodes++;
			}
			int degree; 
			for (T node : network.getNodes()) {
				degree = network.getDegree(node); 
				if (degree >= maxDegree) {
					numberOfMaxDegree++;
					numberOfSufficientDegree++;
				} else if (degree >= minDegree) {
					numberOfSufficientDegree++;
				}
			}
		} else {
			for (T current : network.getNodes()) {
				numberOfNodes ++;
			}
		}
		
		int index, target, numberOfOptions, firstDegree, secondDegree, numberOfUnaccountedNeighbors;
		if (numberOfSufficientDegree < numberOfNodes - 1) {
			for (T first : network.getNodes()) {
				firstDegree = network.getDegree(first);
				while(firstDegree < minDegree) {
					numberOfUnaccountedNeighbors = 0;
					for (T neighbor : network.getAdjacent(first)) {
						if (network.getDegree(neighbor) < minDegree) {
							numberOfUnaccountedNeighbors++;
						}
					}
					numberOfOptions = numberOfNodes - numberOfSufficientDegree - numberOfUnaccountedNeighbors - 1;
					if (numberOfOptions > 0) {
						target = RandomHelper.nextIntFromTo(0, numberOfOptions - 1);
						index = 0;
						for (T second : network.getNodes()) {
							if (first != second) {
								secondDegree = network.getDegree(second);
								if (secondDegree < minDegree && !network.isAdjacent(first, second)) {
									if (index < target) {
										index++;
									} else {
										network.addEdge(first, second);
										firstDegree++;
										if (firstDegree == maxDegree) {
											numberOfMaxDegree++;
											numberOfSufficientDegree++;
										} else if (firstDegree == minDegree) {
											numberOfSufficientDegree++;
										}
										secondDegree++;
										if (secondDegree == maxDegree) {
											numberOfMaxDegree++;
											numberOfSufficientDegree++;
										} else if (secondDegree == minDegree) {
											numberOfSufficientDegree++;
										}
										break;
									}
								}
							}
						}
					} else {
						break;
					}
				}
			}
		}
		
		if (numberOfMaxDegree < numberOfNodes - 1) {
			int numberOfRounds;
			for (T first : network.getNodes()) {
				firstDegree = network.getDegree(first);
				numberOfRounds = RandomHelper.nextIntFromTo(0, maxNumberOfRounds);
				for (int round = 0; round < numberOfRounds && firstDegree < maxDegree; round++) {
					numberOfUnaccountedNeighbors = 0;
					for (T neighbor : network.getAdjacent(first)) {
						if (network.getDegree(neighbor) < maxDegree) {
							numberOfUnaccountedNeighbors++;
						}
					}
					numberOfOptions = numberOfNodes - numberOfMaxDegree - numberOfUnaccountedNeighbors - 1;
					if (numberOfOptions > 0) {
						target = RandomHelper.nextIntFromTo(0, numberOfOptions - 1);
						index = 0;
						for (T second : network.getNodes()) {
							if (first != second) {
								secondDegree = network.getDegree(second);								
								if(secondDegree < maxDegree && !network.isAdjacent(first, second)) {							
									if (index < target) {
										index++;
									} else {
										network.addEdge(first, second);
										firstDegree++;
										if (firstDegree == maxDegree) {
											numberOfMaxDegree++;
										}
										secondDegree++;
										if (secondDegree == maxDegree) {
											numberOfMaxDegree++;
										}
										break;
									}
								}
							}
						}
					} else {
						break;
					}
				}
			}
		}
		return network;
	}
}
