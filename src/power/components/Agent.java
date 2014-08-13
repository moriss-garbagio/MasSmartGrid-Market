package power.components;

import java.util.ArrayList;
import power.SmartGridBuilder;
import power.components.generators.AgentGenerator;
import power.helpers.AdjustedRunningMax;
import power.helpers.RunningMean;
import power.models.IRandomModel;
import power.tools.Absolute;
import power.tools.Clamp;
import power.tools.IDescribable;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.RepastEdge;

/**
 * @author That
 * 
 */
public class Agent implements IDescribable {

	// enumerations
	private enum SimulationPhase {
		Init, Exec, Fin
	}

	private enum Entity {
		Grid, Neighbors, Generation, Storage
	}

	private SimulationPhase phase;
	private int period;

	// Fundamental characteristics
	private final Grid grid;
	private final AgentGenerator group;
	private final IRandomModel suspendableModel;
	private final IRandomModel loadModel;
	private final IRandomModel generationModel;
	private final IReliability reliability;
	private final Storage storage;
	private final int foresight;

	// Agent memory
	private final AdjustedRunningMax demandWindow;
	private final RunningMean deficitWindow;
	private final RunningMean neighborhoodRequestWindow;
	// private final RunningMean priceWindow;

	// Characteristics
	private boolean isConnectedToGrid;
	private double currentlySatisfiedLoad;
	private double currentlySpentGeneration;

	private double suspendedLoad;
	private double currentAddedSuspension;
	private double currentRemovedSuspension;

	// zero by default
	private double currentPowerBoughtFromGrid;
	private double currentPowerBoughtFromNeighbors;
	private double currentPowerSoldToGrid;
	private double currentPowerSoldToNeighbors;
	private double currentNeighborhoodRequest;
	private double currentDumpedPower;
	private double currentForcefullySuspendedLoad;

	// accounting
	// zero by default
	private double currentDebitToGrid;
	private double currentDebitToNeighbors;
	private double currentCreditFromGrid;
	private double currentCreditFromNeighbors;

	// zero by default
	private double currentStorageCost;
	private double currentGenerationCost;

	private static int population = 0;
	private int id;

	public Agent(Grid grid,
			AgentGenerator group,
			IReliability reliability,
			IRandomModel suspendableModel,
			IRandomModel loadModel,
			IRandomModel generatorModel,
			Storage storage,
			int foresight) {
		id = population++;
		isConnectedToGrid = true;
		// System.out.println("Constructing: " + this.toString());

		this.grid = grid;
		this.group = group;

		if (reliability == null) {
			this.reliability = BlankReliability.getBlankReliability();
		} else {
			this.reliability = reliability;
		}

		this.suspendableModel = suspendableModel;
		this.suspendableModel.getModel().setSource(this);

		this.loadModel = loadModel;
		this.loadModel.getModel().setSource(this);

		this.generationModel = generatorModel;
		if (this.generationModel.getModel() != null) {
			this.generationModel.getModel().setSource(this);
		}

		this.storage = storage;
		this.foresight = foresight;

		demandWindow = new AdjustedRunningMax(Absolute.getSingleton());
		deficitWindow = new RunningMean();
		neighborhoodRequestWindow = new RunningMean();
		// priceWindow = grid.getPriceWindow();
	}

	@ScheduledMethod(start = 0, interval = 1, priority = 1)
	public void initialize() {
//		System.err.println("Agent init");
		phase = SimulationPhase.Init;
		period = ((int) RepastEssentials.GetTickCount()) % SmartGridBuilder.getPeriod();

		// clean memoirs
		overAverageDeficit = null;
		underAverageDeficit = null;
		localOverAverageDeficit = null;
		localUnderAverageDeficit = null;
		dynamicOverAverageDeficit = null;
		dynamicUnderAverageDeficit = null;
		localOverAverageRequest = null;
		localUnderAverageRequest = null;
		buyingFromNeighborsAvailability = null;
		neighborhoodRequestRation = null;

		// load
		currentlySatisfiedLoad = 0;
		currentlySpentGeneration = 0;
		currentAddedSuspension = 0;
		currentRemovedSuspension = 0;
		currentForcefullySuspendedLoad = 0;

		currentPowerBoughtFromGrid = 0;
		currentPowerBoughtFromNeighbors = 0;
		currentPowerSoldToGrid = 0;
		currentPowerSoldToNeighbors = 0;
		currentNeighborhoodRequest = 0;
		currentDumpedPower = 0;

		currentStorageCost = storage.getCostFactor() * storage.getCapacity();
		currentGenerationCost = generationModel.getCostFactor() * generationModel.getCurrentValue();

		currentDebitToGrid = 0;
		currentDebitToNeighbors = 0;
		currentCreditFromGrid = 0;
		currentCreditFromNeighbors = 0;

		deficitWindow.add(getDeficit());
	}

	@ScheduledMethod(start = 0, interval = 1, priority = -1)
	public void finalize() {
//		System.err.println("Agent fin");
		phase = SimulationPhase.Fin;

		suspendedLoad = getCurrentSuspendedLoad();
		demandWindow.add(getCurrentDemand());
		neighborhoodRequestWindow.add(getCurrentNeighborhoodRequest());
	}

	@ScheduledMethod(start = 0, interval = 1, priority = 0)
	public void execute() {
//		System.out.println("+" + RepastEssentials.GetTickCount() + ": " + toString());
		phase = SimulationPhase.Exec;

		spendGeneratedPower(satisfyLoad(getCurrentRemainingGeneration()));
		double meanDeficit = deficitWindow.getWindowMean();
		double power = meanDeficit - getDeficit();

		if (meanDeficit > 0) {
			if (getDeficit() == meanDeficit) {
				// 0 < mean == def
				satisfyLoad(buyFromGrid(getCurrentUnsatisfiedLoad())); // ok
				satisfyLoad(buyFromNeighbors(getCurrentUnsatisfiedLoad())); // ok
				satisfyLoad(spendStoredPower(getCurrentUnsatisfiedLoad())); // ok
				suspendLoad(getCurrentUnsatisfiedLoad()); // ok
			} else if (getDeficit() > meanDeficit) {
				// 0 < mean < def
				double powerFromGrid = satisfyLoad(buyFromGrid(meanDeficit)); // ok
				satisfyLoad(buyFromNeighbors(getCurrentUnsatisfiedLoad())); // ok
				
				double battRation = Clamp.getUnit().adjust(getCurrentStoredPower() / getLocalOverAverageDeficit()) * getCurrentUnsatisfiedLoad(); // ok
				spendStoredPower(satisfyLoad(getStoragePowerAvailability(battRation))); // ok
				suspendLoad(getCurrentUnsatisfiedLoad()); // ok
			} else {
				// 0 < def < mean || def <= 0 < mean
				satisfyLoad(buyFromGrid(getCurrentUnsatisfiedLoad())); // ok
				satisfyLoad(buyFromNeighbors(getCurrentUnsatisfiedLoad())); // ok

				Entity[] entities = new Entity[] { Entity.Generation, Entity.Grid, Entity.Neighbors };
				double susRation = (getCurrentSuspendedLoad() / getUnderAverageDeficit()) * power;

				double battRation = power - spendAvailablePower(entities, satisfySuspendedLoad(getAvailablePower(entities, susRation)));
				// double battRation =
				// Clamp.getUnit().adjust(storage.getCurrentAvailableCapacity() /
				// getLocalUnderAverageDeficit()) * power;

				spendAvailablePower(entities, storage.energize(getAvailablePower(entities, battRation)));
			}
		} else {
			if (getDeficit() == meanDeficit) {
				// def = mean <= 0
				spendGeneratedPower(sellToGrid(getCurrentRemainingGeneration()));
				spendGeneratedPower(satisfySuspendedLoad(getCurrentRemainingGeneration()));
				spendGeneratedPower(storage.energize(getCurrentRemainingGeneration()));
			} else if (getDeficit() < meanDeficit) {
				// def < mean <= 0

				Entity[] entities = new Entity[] { Entity.Generation, Entity.Neighbors };
				double susRation = Clamp.getUnit().adjust(getCurrentSuspendedLoad() / getUnderAverageDeficit()) * power;
				double battRation = power - spendAvailablePower(entities, satisfySuspendedLoad(getAvailablePower(entities, susRation)));

				spendAvailablePower(entities, storage.energize(getAvailablePower(entities, battRation)));

				if (SmartGridBuilder.getForceDumping()) {
					spendGeneratedPower(sellToGrid(getGenerationAvailability(-meanDeficit)));
				} else {
					spendGeneratedPower(sellToGrid(getCurrentRemainingGeneration()));
				}

			} else {
				// mean < def < 0 || mean <= 0 <= def
				satisfyLoad(buyFromNeighbors(getCurrentUnsatisfiedLoad()));
				spendGeneratedPower(sellToGrid(getCurrentRemainingGeneration()));

				// power = getCurrentDeficit() - meanDeficit;
				double battRation = -Clamp.getUnit().adjust(getCurrentStoredPower() / getLocalOverAverageDeficit()) * power;
				battRation -= spendStoredPower(satisfyLoad(getStoragePowerAvailability(battRation)));
				satisfySuspendedLoad(getCurrentUnsatisfiedLoad());
				spendStoredPower(sellToGrid(getStoragePowerAvailability(battRation)));

				// spendStoredPower(sellToGrid(getStoragePowerAvailability(-meanDeficit)));
			}
		}

		if (SmartGridBuilder.getCanTrade() &&
				storage.getCurrentAvailableCapacity() > getLocalUnderAverageDeficit() &&
				getMeanNeighborhoodRequest() > getPredictedNeighborhoodRequest()) {
			double request = getMeanNeighborhoodRequest() - getPredictedNeighborhoodRequest();
			double requestRation = Clamp.getUnit().adjust((storage.getCurrentAvailableCapacity() - getLocalUnderAverageDeficit()) / getLocalUnderAverageRequest()) * request;
			
			//System.out.println("availableCapacity:" + storage.getCurrentAvailableCapacity() +", underDeficit:" + getLocalUnderAverageDeficit() + ", underRequest: " + request + ", requestRation:" + requestRation);
			
			double value = spendGeneratedPower(storage.energize(getGenerationAvailability(requestRation)));
			requestRation -= value;
			if (grid.getPredictedBasePrice() < grid.getMeanBasePrice()) {
				value += buyFromGrid(storage.energize(requestRation));
			}
			
			//System.out.println("stored:" + value);
		}

		satisfyLoad(buyFromGrid(getCurrentUnsatisfiedLoad())); // ok
		// Do not request from neighbor to avoid predictions errors
		satisfyLoad(getStoragePowerAvailability(getCurrentUnsatisfiedLoad())); // ok
		spendGeneratedPower(satisfySuspendedLoad(getCurrentRemainingGeneration()));
		spendGeneratedPower(storage.energize(getCurrentRemainingGeneration()));
		spendGeneratedPower(sellToGrid(getCurrentRemainingGeneration()));
		dumpPower(spendGeneratedPower(getCurrentRemainingGeneration())); // ok
		
		forcefullySuspendLoad(getCurrentUnsatisfiedLoad()); // ok
//		System.out.println("-" + RepastEssentials.GetTickCount() + ": " + toString());
	}

	/*
	 * Blackout methods
	 */
	public void reconnectToGrid() {
		isConnectedToGrid = true;
	}

	public void disconnectFromGrid() {
		isConnectedToGrid = false;
	}

	public boolean isConnectedToGrid() {
		return isConnectedToGrid;
	}

	/*
	 * interaction methods
	 */
	private double getBuyingFromGridAvailability(double power) {
		if (!isConnectedToGrid)
			return 0.0;
		return power;
	}

	private double buyFromGrid(double power) {
		if (!isConnectedToGrid)
			return 0.0;
		double powerBought = grid.buyFromGrid(power);
		currentPowerBoughtFromGrid += powerBought;
		currentDebitToGrid += grid.getSellingValue(powerBought);
		return powerBought;
	}

	private double getSellingToGridAvailability(double power) {
		if (!isConnectedToGrid)
			return 0.0;
		return grid.getSellToGridAvailability(power);
	}

	private double sellToGrid(double power) {
		if (!isConnectedToGrid)
			return 0.0;

		double soldPower = grid.sellToGrid(power);
		currentPowerSoldToGrid += soldPower;
		currentCreditFromGrid += grid.getBuyingValue(soldPower);
		return soldPower;
	}

	private Double buyingFromNeighborsAvailability;

	private double getBuyingFromNeighborsAvailability() {
		if (buyingFromNeighborsAvailability == null) {
			double available = 0;
			for (Object obj : SmartGridBuilder.getNetwork().getAdjacent(this)) {
				if (obj instanceof Agent) {
					Agent agent = (Agent) obj;
					available = agent.getSellingToNeighborsAvailability(this);
				}
			}
			buyingFromNeighborsAvailability = available;
		}

		return buyingFromNeighborsAvailability;
	}

	private double getBuyingFromNeighborsAvailability(double request) {
		if (request > getBuyingFromNeighborsAvailability())
			return getBuyingFromNeighborsAvailability();
		return request;
	}

	private double buyFromNeighbors(double request) {
		ArrayList<Agent> neighbors = new ArrayList<Agent>(SmartGridBuilder.getNetwork().getDegree(this));
		for (Object obj : SmartGridBuilder.getNetwork().getAdjacent(this)) {
			if (obj instanceof Agent) {
				Agent neighbor = (Agent) obj;
				neighbors.add(neighbor);
			}
		}

		double available = 0;
		for (int index = neighbors.size() - 1; index >= 0; index--) {
			if (request <= 0)
				break;
			int random = RandomHelper.nextIntFromTo(0, index);
			Agent neighbor = neighbors.get(random);
			if (random != index) {
				neighbors.set(random, neighbors.get(index));
				neighbors.set(index, neighbor);
			}

			double power = neighbor.requestToBuy(this, request);
			request -= power;
			available += power;
		}

		if (buyingFromNeighborsAvailability != null)
			buyingFromNeighborsAvailability -= available;
		currentPowerBoughtFromNeighbors += available;
		currentDebitToNeighbors = grid.getBaseValue(currentPowerBoughtFromNeighbors);
		return available;
	}

	private double getSellingToNeighborsAvailability(Agent requester) {
		if (!reliability.isOperational() ||
				!SmartGridBuilder.getCanTrade() || 
				grid.getPredictedBasePrice() <= grid.getMeanBasePrice() ||
				getCurrentStoredPower() <= getLocalOverAverageDeficit() ||
				getMeanNeighborhoodRequest() >= getPredictedNeighborhoodRequest() ||
				currentNeighborhoodRequest >= getNeighborhoodRequestRation())
		return 0.0;
		
		return getStoragePowerAvailability(getNeighborhoodRequestRation() - currentNeighborhoodRequest);
	}

	private double getSellingToNeighborsAvailability(Agent requester, double power) {
		if (!reliability.isOperational() ||
				!SmartGridBuilder.getCanTrade() || 
				grid.getPredictedBasePrice() <= grid.getMeanBasePrice() ||
				getCurrentStoredPower() <= getLocalOverAverageDeficit() ||
				getMeanNeighborhoodRequest() >= getPredictedNeighborhoodRequest() ||
				currentNeighborhoodRequest >= getNeighborhoodRequestRation())
		return 0.0;
		 
		if (currentNeighborhoodRequest + power > getNeighborhoodRequestRation()) {
			power = getNeighborhoodRequestRation() - currentNeighborhoodRequest;
		}

		return getStoragePowerAvailability(power);
	}

	private Double neighborhoodRequestRation = null;

	private double getNeighborhoodRequestRation() {
		if (neighborhoodRequestRation == null) {
			neighborhoodRequestRation = Clamp.getUnit().adjust((getStoredPower() - getLocalOverAverageDeficit()) / getLocalOverAverageRequest()) * (getPredictedNeighborhoodRequest() - getMeanNeighborhoodRequest());
		}
		return neighborhoodRequestRation;
	}

	/**
	 * The agent check if it has generated a surplus of power, if so the agent
	 * returns some as much requested power as possible from the surplus Note:
	 * This agent and the requester should be neighbors
	 * 
	 * @param request
	 *            The amount of power being requested to be bought
	 * @return the amount of power bought
	 */
	private double requestToBuy(Agent requester, double request) {
//		System.out.println("+requestToBuy(" + requester.toString() + ", " + request + ")");
		double power = storage.draw(getSellingToNeighborsAvailability(requester, request));
		
		currentNeighborhoodRequest += request;
		currentPowerSoldToNeighbors += power;
		currentCreditFromNeighbors = grid.getBaseValue(currentPowerSoldToNeighbors);
		if (power > 0) {
			RepastEdge<Object> edge = SmartGridBuilder.getNetwork().getEdge(requester, this);
			if (edge != null) {
				SmartGridBuilder.getNetwork().removeEdge(edge);
				SmartGridBuilder.getNetwork().addEdge(this, requester);
			}
			edge = SmartGridBuilder.getNetwork().getEdge(this, requester);
			edge.setWeight(power);
		}
//		System.out.println("-requestToBuy(): " + power);
		return power;
	}

	// End of interaction method segment

	/*
	 * Start of computations section
	 */
	private Double underAverageDeficit = null;

	private double getUnderAverageDeficit() {
		if (underAverageDeficit == null) {
			underAverageDeficit = getUnderBaselineDeficit(getMeanDeficit());
		}
		return underAverageDeficit;
	}

	private double getUnderBaselineDeficit(double baseline) {
		double underBaselineDeficit = 0.0;
		for (int future = 0; future < foresight; future++) {
			underBaselineDeficit += Clamp.getDefault().adjust(baseline - getPredictedDeficit(future));
		}
		return underBaselineDeficit;
	}

	private Double localUnderAverageDeficit = null;

	private double getLocalUnderAverageDeficit() {
		if (localUnderAverageDeficit == null) {
			localUnderAverageDeficit = getLocalUnderBaselineDeficit(getMeanDeficit());
		}
		return localUnderAverageDeficit;
	}

	private double getLocalUnderBaselineDeficit(double baseline) {
		double underBaselineDeficit = 0.0;
		int future = 0;
		while (getPredictedDeficit(future) < baseline && future < foresight) {
			underBaselineDeficit += baseline - getPredictedDeficit(future);
			future++;
		}
		return underBaselineDeficit;
	}

	private Double dynamicUnderAverageDeficit = null;

	private double getDynamicUnderAverageDeficit() {
		if (dynamicUnderAverageDeficit == null) {
			dynamicUnderAverageDeficit = getDynamicUnderBaselineDeficit(getMeanNeighborhoodRequest());
		}
		return dynamicUnderAverageDeficit;
	}

	private double getDynamicUnderBaselineDeficit(double baseline) {
		double underBaselineDeficit = 0.0;
		double maxUnderBaselineDeficit = Double.NEGATIVE_INFINITY;
		for (int future = 0; future < foresight; future++) {
			underBaselineDeficit += baseline - getPredictedDeficit(future);
			if (underBaselineDeficit > maxUnderBaselineDeficit) {
				maxUnderBaselineDeficit = underBaselineDeficit;
			}
		}
		return maxUnderBaselineDeficit;
	}

	private Double localUnderAverageRequest = null;

	private double getLocalUnderAverageRequest() {
		if (localUnderAverageRequest == null) {
			localUnderAverageRequest = getLocalUnderBaselineRequest(getMeanNeighborhoodRequest());
		}
		return localUnderAverageRequest;
	}

	private double getLocalUnderBaselineRequest(double baseline) {
		double underBaselineRequest = 0.0;
		int future = 0;
		while (getPredictedNeighborhoodRequest(future) < baseline && future < foresight) {
			underBaselineRequest += baseline - getPredictedNeighborhoodRequest(future);
			future++;
		}
		return underBaselineRequest;
	}

	private Double overAverageDeficit = null;

	private double getOverAverageDeficit() {
		if (overAverageDeficit == null) {
			overAverageDeficit = getOverBaselineDeficit(getMeanDeficit());
		}
		return overAverageDeficit;
	}

	private double getOverBaselineDeficit(double baseline) {
		double overBaselineDeficit = 0.0;
		for (int future = 0; future < foresight; future++) {
			overBaselineDeficit += Clamp.getDefault().adjust(baseline - getPredictedDeficit(future));
		}
		return overBaselineDeficit;
	}

	private Double localOverAverageDeficit = null;

	private double getLocalOverAverageDeficit() {
		if (localOverAverageDeficit == null) {
			localOverAverageDeficit = getLocalOverBaselineDeficit(getMeanDeficit());
		}
		return localOverAverageDeficit;
	}

	private double getLocalOverBaselineDeficit(double baseline) {
		double overBaselineDeficit = 0.0;
		int future = 0;
		while (getPredictedDeficit(future) > baseline && future < foresight) {
			overBaselineDeficit += getPredictedDeficit(future) - baseline;
			future++;
		}
		return overBaselineDeficit;
	}

	private Double dynamicOverAverageDeficit = null;

	private double getDynamicOverAverageDeficit() {
		if (dynamicOverAverageDeficit == null) {
			dynamicOverAverageDeficit = getDynamicOverBaselineDeficit(getMeanDeficit());
		}
		return dynamicOverAverageDeficit;
	}

	private double getDynamicOverBaselineDeficit(double baseline) {
		double overBaselineDeficit = 0.0;
		double maxOverBaselineDeficit = Double.NEGATIVE_INFINITY;
		for (int future = 0; future < foresight; future++) {
			overBaselineDeficit += getPredictedDeficit(future) - baseline;
			if (overBaselineDeficit > maxOverBaselineDeficit) {
				maxOverBaselineDeficit = overBaselineDeficit;
			}
		}
		return maxOverBaselineDeficit;
	}

	private Double localOverAverageRequest = null;

	private double getLocalOverAverageRequest() {
		if (localOverAverageRequest == null) {
			localOverAverageRequest = getLocalOverBaselineRequest(getMeanNeighborhoodRequest());
		}
		return localOverAverageRequest;
	}

	private double getLocalOverBaselineRequest(double baseline) {
		double overBaselineRequest = 0.0;
		int future = 0;
		while (getPredictedNeighborhoodRequest(future) > baseline && future < foresight) {
			overBaselineRequest += getPredictedNeighborhoodRequest(future) - baseline;
			future++;
		}
		return overBaselineRequest;
	}

	// end of computations section

	/*
	 * Start of helper method section
	 */
	private double getAvailablePower(Entity[] sourceList, double request) {
		double available = 0;
		for (Entity entity : sourceList) {
			if (request <= 0)
				return available;
			double power = 0;
			switch (entity) {
			case Generation:
				power = getGenerationAvailability(request);
				break;
			case Grid:
				power = getBuyingFromGridAvailability(request);
				break;
			case Neighbors:
				power = getBuyingFromNeighborsAvailability(request);
				break;
			case Storage:
				power = getStoragePowerAvailability(request);
				break;
			}
			request -= power;
			available += power;
		}
		return available;
	}

	private double spendAvailablePower(Entity[] sourceList, double request) {
		double supplied = 0;
		for (Entity entity : sourceList) {
			if (request <= 0)
				return supplied;
			double power = 0;
			switch (entity) {
			case Generation:
				power = spendGeneratedPower(request);
				break;
			case Grid:
				power = buyFromGrid(request);
				break;
			case Neighbors:
				power = buyFromNeighbors(request);
				break;
			case Storage:
				power = spendStoredPower(request);
				break;
			}
			request -= power;
			supplied += power;
		}
		return supplied;
	}

	/**
	 * @param power
	 *            the amount of load to satisfy
	 * @return the power which was used
	 */
	private double satisfyLoad(double power) {
		double usedPower;
		if (power < getCurrentUnsatisfiedLoad()) {
			usedPower = power;
		} else {
			usedPower = getCurrentUnsatisfiedLoad();
		}
		currentlySatisfiedLoad += usedPower;
		return usedPower;
	}

	/**
	 * @param power
	 *            the amount of suspended load to satisfy
	 * @return the power which was used
	 */
	private double satisfySuspendedLoad(double power) {
		double usedPower;
		if (power < getCurrentSuspendedLoad()) {
			usedPower = power;
		} else {
			usedPower = getCurrentSuspendedLoad();
		}
		currentRemovedSuspension += usedPower;
		return usedPower;
	}

	private double forcefullySuspendLoad(double power) {
		double load = satisfyLoad(power);
		currentAddedSuspension += load;
		currentForcefullySuspendedLoad += load;
		return load;
	}

	/**
	 * @param power
	 *            the amount of power to suspend
	 * @return the amount of power suspended
	 */
	private double suspendLoad(double power) {
		if (SmartGridBuilder.getCanSuspendLoad()) {
			if (getCurrentRemainingSuspendableLoad() < power) {
				power = getCurrentRemainingSuspendableLoad();
			}

			double load = satisfyLoad(power);
			currentAddedSuspension += load;
			return load;
		} else {
			return 0;
		}
	}

	/**
	 * @param power
	 *            the amount of power to dump
	 * @return the amount of power dumped
	 */
	private double dumpPower(double power) {
		currentDumpedPower += power;
		return power;
	}

	/**
	 * @param power
	 * @return the power which was used
	 */
	private double getGenerationAvailability(double power) {
		double available = power;
		if (power > getCurrentRemainingGeneration()) {
			available = getCurrentRemainingGeneration();
		}
		return available;
	}

	/**
	 * @param power
	 * @return the power which was used
	 */
	private double spendGeneratedPower(double power) {
		double usedPower = power;
		if (power > getCurrentRemainingGeneration()) {
			usedPower = getCurrentRemainingGeneration();
		}
		currentlySpentGeneration += usedPower;
		return usedPower;
	}

	private double getStoragePowerAvailability(double power) {
		double available = power;
		if (storage.getCurrentPower() < power) {
			available = storage.getCurrentPower();
		}
		return available;
	}

	private double spendStoredPower(double power) {
		return storage.draw(power);
	}

	// End of helper methods section

	/*
	 * Value methods
	 */
	private double getCurrentUnsatisfiedLoadAvailability(double power) {
		if (power > getCurrentUnsatisfiedLoad())
			return getCurrentUnsatisfiedLoad();
		return power;
	}

	private double getCurrentUnsatisfiedLoad() {
		return loadModel.getCurrentValue() - currentlySatisfiedLoad;
	}

	private double getCurrentRemainingGeneration() {
		return generationModel.getCurrentValue() - currentlySpentGeneration;
	}

	private double getCurrentDeficit() {
		return getCurrentUnsatisfiedLoad() - getCurrentRemainingGeneration();
	}

	private double getCurrentSuspendedLoad() {
		return suspendedLoad + getCurrentChangeInSuspention();
	}

	// End of value methods section

	@Override
	public String toString() {
		return group.getName() + "-" + id;
	}

	@Override
	public String description() {
		return this.description(0);
	}

	@Override
	public String description(int nestingLevel) {
		String tabbing = "";
		if (nestingLevel > 0)
			tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
		String str = "Agent: {\n\t" + tabbing + "id: " + id + "\n\t" + tabbing + "foresight: " + foresight + "\n\t" + tabbing + "generationModel: " + (generationModel == null ? "null\n" : generationModel.description(nestingLevel + 1)) + "\t" + tabbing + "loadModel: " + (loadModel == null ? "null\n" : loadModel.description(nestingLevel + 1)) + "\t" + tabbing + "storage: " + (storage == null ? "null\n" : storage.description(nestingLevel + 1)) + "\t" + tabbing + "suspendableModel: " + (suspendableModel == null ? "null\n" : suspendableModel.description(nestingLevel + 1)) + tabbing + "}\n";
		return str;
	}

	/*
	 * Windows
	 */
	public RunningMean getDemandWindow() {
		return demandWindow;
	}

	public double getMaxDemandOfPeriod() {
		return demandWindow.getRecentMax();
	}

	// end of windows

	public double getMeanLoadSeed() {
		return loadModel.getMeanSeedValue(); // this should not be NaN
	}

	/*
	 * Start of state
	 */
	public double getCurrentPowerAddedToStore() {
		return storage.getCurrentPowerAdded();
	}

	public double getCurrentPowerRemovedFromStore() {
		return storage.getCurrentPowerRemoved();
	}

	public double getCurrentStoredPower() {
		return storage.getCurrentPower();
	}

	public double getStoredPower() {
		return storage.getPower();
	}

	public double getStorageCapacity() {
		return storage.getCapacity();
	}

	public double getLoad() {
		return loadModel.getCurrentValue();
	}

	public double getGeneration() {
		return generationModel.getCurrentValue();
	}

	public double getSuspendableLoad() {
		if (SmartGridBuilder.getCanSuspendLoad())
			return suspendableModel.getCurrentValue() * getLoad();
		return 0;
	}

	public double getCurrentRemainingSuspendableLoad() {
		return getSuspendableLoad() - getCurrentChangeInSuspention();
	}

	public double getCurrentAddedSuspension() {
		return currentAddedSuspension;
	}

	public double getCurrentRemovedSuspension() {
		return currentRemovedSuspension;
	}

	public double getCurrentChangeInSuspention() {
		return currentAddedSuspension - currentRemovedSuspension;
	}

	public double getSuspendedLoad() {
		return suspendedLoad;
	}

	public double getCurrentForcefullySuspendedLoad() {
		return currentForcefullySuspendedLoad;
	}
	
	public double getUnsatisfiedLoadCount() {
		return currentForcefullySuspendedLoad > 0 ? 1 : 0; 
	}

	public double getCurrentDumpedPower() {
		return currentDumpedPower;
	}

	// end of internal state

	/*
	 * Power accounts
	 */
	public double getCurrentPowerBoughtFromGrid() {
		return currentPowerBoughtFromGrid;
	}

	public double getCurrentPowerBoughtFromNeighbors() {
		return currentPowerBoughtFromNeighbors;
	}

	public double getCurrentPowerSoldToGrid() {
		return currentPowerSoldToGrid;
	}

	public double getCurrentPowerSoldToNeighbors() {
		return currentPowerSoldToNeighbors;
	}

	// end of power accounts

	/*
	 * Monetary accounts
	 */
	public double getCurrentDebitToGrid() {
		return currentDebitToGrid;
	}

	public double getCurrentDebitToNeighbors() {
		return currentDebitToNeighbors;
	}

	public double getCurrentCreditFromGrid() {
		return currentCreditFromGrid;
	}

	public double getCurrentCreditFromNeighbors() {
		return currentCreditFromNeighbors;
	}

	public double getCurrentProfitFromNeigbors() {
		return currentCreditFromNeighbors - currentDebitToNeighbors;
	}

	public double getCurrentProfitFromGrid() {
		return currentCreditFromGrid - currentDebitToGrid;
	}

	public double getCurrentProfit() {
		return getCurrentProfitFromGrid() + getCurrentProfitFromNeigbors() - getCurrentGenerationCost() - getCurrentStorageCost();
	}

	// end of monetary accounts

	/*
	 * Start of cost functions
	 */
	public double getCurrentGenerationCost() {
		return currentGenerationCost;
	}

	public double getCurrentStorageCost() {
		return currentStorageCost;
	}

	// End of costs

	// FIX NOW
	public double getCurrentHomeElectricityRate() {
		return getCurrentProfit() / (0.001 + getCurrentChangeInSuspention() - getLoad());
	}

	/*
	 * Deficits
	 */
	public double getDeficit() {
		return loadModel.getCurrentValue() - generationModel.getCurrentValue();
	}

	public double getPredictedDeficit(int foresight) {
		if (foresight == 0) {
			return getDeficit();
		} else {
			return deficitWindow.getPeriodMean(foresight);
		}
	}

	public double getMeanDeficit() {
		return deficitWindow.getWindowMean();
	}

	public double getCurrentDemand() {
		return currentPowerBoughtFromGrid - currentPowerSoldToGrid;
	}

	// end of demand

	/*
	 * Neighborhood State
	 */
	public double getCurrentNeighborhoodRequest() {
		return currentNeighborhoodRequest;
	}
	
	public String getCurrentNeighborhoodRequestLabel() {
		return "Current Neighborhood Request of " + group.toString();
	}

	public double getPredictedNeighborhoodRequest() {
		return neighborhoodRequestWindow.getPeriodMean(0);
	}
	
	public String getPredictedNeighborhoodRequestLabel() {
		return "Predicted Neighborhood Request of " + group.toString();
	}

	private double getPredictedNeighborhoodRequest(int foresight) {
		return neighborhoodRequestWindow.getPeriodMean(foresight);
	}

	public double getMeanNeighborhoodRequest() {
		return neighborhoodRequestWindow.getWindowMean();
	}
	
	public String getMeanNeighborhoodRequestLabel() {
		return "Mean Neighborhood Request of " + group.toString();
	}
	// end of neighborhood state
}