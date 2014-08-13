package power.components.generators;

import java.util.LinkedList;

import org.w3c.dom.Node;

import power.SmartGridBuilder;
import power.components.Grid;
import power.components.Agent;
import power.components.IReliability;
import power.components.Storage;
import power.distributions.IRandomDistribution;
import power.distributions.RandomDistribution;
import power.helpers.AdjustedRunningMax;
import power.helpers.Computations;
import power.helpers.RunningMean;
import power.helpers.XmlTools;
import power.models.BlankRandomModel;
import power.models.IRandomModel;
import power.tools.Absolute;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;

/**
 * @author That
 * 
 */
public class AgentGenerator implements IGenerator<Agent> {

	private enum XmlNode {
		UtilityModelGenerator,
		SuspendableModelGenerator,
		LoadModelGenerator,
		GenerationModelGenerator,
		StorageGenerator,
		PowerContractGenerator,
		ForesightRandomParameter
	}

	private enum XmlAttribute {
		population
	}

	private int period;

	private Grid grid;
	private String name;
	private int population;

	private IGenerator<IReliability> reliabilityGenerator;
	private IGenerator<IRandomModel> suspendableModelGenerator;
	private IGenerator<IRandomModel> loadModelGenerator;
	private IGenerator<IRandomModel> generationModelGenerator;
	private StorageGenerator storageGenerator;
	private IRandomDistribution foresightGenerator;

	private final LinkedList<Agent> agentList = new LinkedList<Agent>();

	// factors
	private final AdjustedRunningMax demandWindow;
	private final RunningMean deficitWindow;

	private double loadFactor;
	private double diversityFactor;

	private Double loadFactorHistory;
	private Double diversityFactorHistory;

	private Double profitHistory = null;

	public static AgentGenerator create(Node xml) {
		if (xml == null)
			return null;
		String name = XmlTools.getHardAttributeValue(xml, XmlTools.XmlAttribute.name);
		int population = Integer.parseInt(XmlTools.getHardAttributeValue(xml, XmlAttribute.population));

		IGenerator<IReliability> reliabilityGenerator = new ReliabilityGenerator(xml);
		IGenerator<IRandomModel> suspendableModelGenerator = RandomModelGenerator.create(XmlTools.getUptoOneNode(xml, XmlNode.SuspendableModelGenerator));
		IGenerator<IRandomModel> loadModelGenerator = RandomModelGenerator.create(XmlTools.getUptoOneNode(xml, XmlNode.LoadModelGenerator));
		IGenerator<IRandomModel> generationModelGenerator = RandomModelGenerator.create(XmlTools.getUptoOneNode(xml, XmlNode.GenerationModelGenerator));
		StorageGenerator storageGenerator = StorageGenerator.create(XmlTools.getUptoOneNode(xml, XmlNode.StorageGenerator));
		IRandomDistribution foresightGenerator = RandomDistribution.create(XmlTools.getUptoOneNode(xml, XmlNode.ForesightRandomParameter));

		return new AgentGenerator(name, population, reliabilityGenerator, suspendableModelGenerator, loadModelGenerator, generationModelGenerator, storageGenerator, foresightGenerator);
	}

	public AgentGenerator(String name,
			int population,
			IGenerator<IReliability> reliabilityGenerator,
			IGenerator<IRandomModel> suspendableModelGenerator,
			IGenerator<IRandomModel> loadModelGenerator,
			IGenerator<IRandomModel> generationModelGenerator,
			StorageGenerator storageGenerator,
			IRandomDistribution foresightGenerator) {

		this.grid = SmartGridBuilder.getGrid();
		this.name = name;
		this.population = population;
		this.reliabilityGenerator = reliabilityGenerator;
		this.suspendableModelGenerator = suspendableModelGenerator;
		this.loadModelGenerator = loadModelGenerator;
		this.generationModelGenerator = generationModelGenerator;
		this.storageGenerator = storageGenerator;
		this.foresightGenerator = foresightGenerator;

		this.demandWindow = new AdjustedRunningMax(Absolute.getSingleton());
		this.deficitWindow = new RunningMean();
	}

	@Override
	public Agent create() {

		IReliability reliability = null;
		if (reliabilityGenerator != null) {
			reliability = reliabilityGenerator.create();
		}

		IRandomModel suspendableModel = null;
		if (suspendableModelGenerator == null) {
			suspendableModel = BlankRandomModel.getBlankRandomModel();
		} else {
			suspendableModel = suspendableModelGenerator.create();
		}

		IRandomModel loadModel = null;
		if (loadModelGenerator == null) {
			loadModel = BlankRandomModel.getBlankRandomModel();
		} else {
			loadModel = loadModelGenerator.create();
		}

		IRandomModel generationModel = null;
		if (generationModelGenerator == null) {
			generationModel = BlankRandomModel.getBlankRandomModel();
		} else {
			generationModel = generationModelGenerator.create();
		}

		Storage storage = null;
		if (storageGenerator == null) {
			storage = Storage.getDefaultStorage();
		} else {
			storage = storageGenerator.create();
		}

		int forsight = 0;
		if (foresightGenerator != null) {
			forsight = foresightGenerator.nextInt();
		}

		Agent agent = new Agent(grid, this, reliability, suspendableModel, loadModel, generationModel, storage, forsight);

		agentList.add(agent);
		SmartGridBuilder.getContext().add(agent);

		return agent;
	}

	public Agent remove() {
		Agent agent = agentList.removeLast();
		SmartGridBuilder.getContext().remove(agent);
		return agent;
	}

	public Agent[] createAll() {
		resetPopulation();
		return (Agent[]) agentList.toArray(new Agent[agentList.size()]);
	}

	public void resetPopulation() {
		agentList.clear();
		adjustPopulation();
	}

	private void adjustPopulation() {
		if (population != agentList.size()) {
			if (population < 0) {
				System.err.println("The population of " + name + " is negative.");
				(new Exception()).printStackTrace();
				System.exit(1);
			}
			while (population > agentList.size()) {
				create();
			}
			while (population < agentList.size()) {
				remove();
			}
		}
	}

	@Override
	public String description() {
		return this.description(0);
	}

	// TODO
	@Override
	public String description(int nestingLevel) {
		String tabbing = "";
		if (nestingLevel > 0)
			tabbing = new String(new char[nestingLevel]).replace("\0", "\t");
		String str = "AgentGenerator: {\n\t" + tabbing + "name: " + name + "\n\t" + tabbing + "population: " + population + "\n\t" + tabbing + "foresightGenerator: " + (foresightGenerator == null ? "null\n" : foresightGenerator.description(nestingLevel + 1)) + "\t" + tabbing + "generationModelGenerator: " + (generationModelGenerator == null ? "null\n" : generationModelGenerator.description(nestingLevel + 1)) + "\t" + tabbing + "loadModelGenerator: " + (loadModelGenerator == null ? "null\n" : loadModelGenerator.description(nestingLevel + 1)) + "\t" + tabbing + "reliabilityGenerator: " + (reliabilityGenerator == null ? "null\n" : reliabilityGenerator.description(nestingLevel + 1)) + "\n\t" + tabbing + "storageGenerator: " + (storageGenerator == null ? "null\n" : storageGenerator.description(nestingLevel + 1)) + "\t" + tabbing + "suspendableModelGenerator: " + (suspendableModelGenerator == null ? "null\n" : suspendableModelGenerator.description(nestingLevel + 1)) + tabbing + "}\n";
		return str;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPopulation() {
		return population;
	}

	public void setPopulation(int population) {
		if (this.population != population) {
			this.population = population;
			adjustPopulation();
		}
	}

	public LinkedList<Agent> getAgents() {
		return agentList;
	}

	/*
	 * Start of generation methods
	 */
	public IGenerator<IRandomModel> getSuspendableModelGenerator() {
		return suspendableModelGenerator;
	}

	public void setSuspendableModelGenerator(IGenerator<IRandomModel> suspendableModelGenerator) {
		this.suspendableModelGenerator = suspendableModelGenerator;
	}

	public IGenerator<IRandomModel> getLoadModelGenerator() {
		return loadModelGenerator;
	}

	public void setLoadModelGenerator(IGenerator<IRandomModel> loadModelGenerator) {
		this.loadModelGenerator = loadModelGenerator;
	}

	public IGenerator<IRandomModel> getGenerationModelGenerator() {
		return generationModelGenerator;
	}

	public void setGenerationModelGenerator(IGenerator<IRandomModel> generationModelGenerator) {
		this.generationModelGenerator = generationModelGenerator;
	}

	public StorageGenerator getStorageGenerator() {
		return storageGenerator;
	}

	public void setStorageGenerator(StorageGenerator storageGenerator) {
		this.storageGenerator = storageGenerator;
	}

	public IGenerator<IReliability> getReliabilityGenerator() {
		return reliabilityGenerator;
	}

	public void setReliabilityGenerator(IGenerator<IReliability> reliabilityGenerator) {
		this.reliabilityGenerator = reliabilityGenerator;
	}

	public IRandomDistribution getForesightGenerator() {
		return foresightGenerator;
	}

	public void setForesightGenerator(RandomDistribution foresightGenerator) {
		this.foresightGenerator = foresightGenerator;
	}

	// end of generation methods

	/*
	 * Demand functions
	 */
	public double getPredictedDemandByLinearAvg() {
		return getPredictedDemandByPeriod(0);
	}

	public String getPredictedDemandByPeriodLabel() {
		return "Predicted demand by linear avg. of " + name;
	}

	public double getPredictedDemandByPeriod(int foresight) {
		return demandWindow.getPeriodMean(foresight);
	}

	// end of demand functions

	@ScheduledMethod(start = 0, interval = 1, priority = 2)
	public void initialize() {
		period = ((int) RepastEssentials.GetTickCount()) % SmartGridBuilder.getPeriod();

		// flush memoirs
		currentPowerBoughtFromGrid = null;
		currentPowerSoldToGrid = null;
		load = null;
		generation = null;
		storedPower = null;
		currentAddedSuspension = null;
		currentRemovedSuspension = null;
		suspendedLoad = null;
		currentNeighborhoodRequest = null;
		meanNeighborhoodRequest = null;
		currentPowerSoldToNeighbors = null;
		currentProfit = null;
		maxDemandOfPeriod = null;

		deficitWindow.add(getDeficit());
	}

	@ScheduledMethod(start = 0, interval = 1, priority = -2)
	public void finalize() {
		profitHistory = Computations.getExponentialAverage(profitHistory, getCurrentProfit());

		demandWindow.add(getCurrentDemand());

		double max = demandWindow.getRecentMax();
		double mean = demandWindow.getRecentMean();

		loadFactor = Math.abs(mean / max);
		loadFactorHistory = Computations.getExponentialAverage(loadFactorHistory, loadFactor);

		diversityFactor = getMaxDemandOfPeriod() / max;
		diversityFactorHistory = Computations.getExponentialAverage(diversityFactorHistory, diversityFactor);
	}

	/*
	 * Windows
	 */
	public RunningMean getDemandWindow() {
		return demandWindow;
	}

	private Double maxDemandOfPeriod;

	public double getMaxDemandOfPeriod() {
		if (maxDemandOfPeriod == null) {
			double sum = 0;
			for (Agent agent : agentList) {
				sum += agent.getMaxDemandOfPeriod();
			}
			maxDemandOfPeriod = sum / getPopulation();
		}
		return maxDemandOfPeriod;
	}

	// end of windows

	public double getMeanLoadFactor() {
		return loadFactorHistory == null ? 0 : loadFactorHistory;
	}

	public String getMeanLoadFactorLabel() {
		return "Mean load factor of " + name;
	}

	public double getMeanDiversityFactor() {
		return diversityFactorHistory == null ? 0 : diversityFactorHistory;
	}

	public String getMeanDiversityFactorLabel() {
		return "Mean diversity factor of " + name;
	}

	/*
	 * Statistical properties
	 */
	public double getLoadFactor() {
		return loadFactor;
	}

	public String getLoadFactorLabel() {
		return "Load factor of " + name;
	}

	public double getDiversityFactor() {
		return diversityFactor;
	}

	public String getDiversityFactorLabel() {
		return "Diversity factor of " + name;
	}

	private Double currentPowerBoughtFromGrid;

	public double getCurrentPowerBoughtFromGrid() {
		if (currentPowerBoughtFromGrid == null) {
			double sum = 0.0;
			for (Agent agent : agentList) {
				sum += agent.getCurrentPowerBoughtFromGrid();
			}
			currentPowerBoughtFromGrid = sum / getPopulation();
		}
		return currentPowerBoughtFromGrid;
	}

	public String getCurrentPowerBoughtFromGridLabel() {
		return "Current power bought from grid by " + name;
	}

	private Double currentPowerSoldToGrid;

	public double getCurrentPowerSoldToGrid() {
		if (currentPowerSoldToGrid == null) {
			double sum = 0.0;
			for (Agent agent : agentList) {
				sum += agent.getCurrentPowerSoldToGrid();
			}
			currentPowerSoldToGrid = sum / getPopulation();
		}
		return currentPowerSoldToGrid;
	}

	public String getCurrentPowerSoldToGridLabel() {
		return "Current power sold to grid by " + name;
	}

	public double getDemandSTD() {
		return demandWindow.recomputeStandardDev((int) SmartGridBuilder.getPeriod());
	}

	public String getDemandSTDLabel() {
		return "Standard deviation for " + name;
	}
	
	public double getDemandVariance() {
		return demandWindow.recomputeVariance((int) SmartGridBuilder.getPeriod());
	}

	public String getDemandVarianceLabel() {
		return "Variation for " + name;
	}
	
	public double getDemandCV() {
		return demandWindow.recomputeStandardDev((int) SmartGridBuilder.getPeriod()) / Math.abs(demandWindow.getRecentMean());
	}

	public String getDemandCVLabel() {
		return "Coefficient of variation for " + name;
	}

	private Double load;

	public double getLoad() {
		if (load == null) {
			double sum = 0;
			for (Agent agent : agentList) {
				sum += agent.getLoad();
			}
			load = sum / getPopulation();
		}
		return load;
	}

	public String getLoadLabel() {
		return "Load of " + name;
	}

	private Double generation;

	public double getGeneration() {
		if (generation == null) {
			double sum = 0;
			for (Agent agent : agentList) {
				sum += agent.getGeneration();
			}
			generation = sum / getPopulation();
		}
		return generation;
	}

	public String getGenerationLabel() {
		return "Generation of " + name;
	}

	public double getDeficit() {
		return getLoad() - getGeneration();
	}

	public String getDeficitLabel() {
		return "Deficit of " + name;
	}

	public double getMeanDeficit() {
		return deficitWindow.getWindowMean();
	}

	public String getMeanDeficitLabel() {
		return "Mean deficit of " + name;
	}

	public double getCurrentDemand() {
		return getCurrentPowerBoughtFromGrid() - getCurrentPowerSoldToGrid();
	}

	public String getCurrentDemandLabel() {
		return "Current demand of " + name + " from grid";
	}

	public double getMeanDemand() {
		return demandWindow.getWindowMean();
	}

	public String getMeanDemandLabel() {
		return "Mean demand of " + name;
	}

	private Double storedPower;

	public double getStoredPower() {
		if (storedPower == null) {
			double sum = 0;
			for (Agent agent : agentList) {
				sum += agent.getStoredPower();
			}
			storedPower = sum / getPopulation();
		}
		return storedPower;
	}

	public String getStoredPowerLabel() {
		return "Stored power of " + name;
	}

	private Double currentAddedSuspension;

	public double getCurrentAddedSuspension() {
		if (currentAddedSuspension == null) {
			double sum = 0;
			for (Agent agent : agentList) {
				sum += agent.getCurrentAddedSuspension();
			}
			currentAddedSuspension = sum / getPopulation();
		}
		return currentAddedSuspension;
	}

	public String getCurrentAddedSuspensionLabel() {
		return "Load added to suspension by " + name;
	}

	private Double currentRemovedSuspension;

	public double getCurrentRemovedSuspension() {
		if (currentRemovedSuspension == null) {
			double sum = 0;
			for (Agent agent : agentList) {
				sum += agent.getCurrentRemovedSuspension();
			}
			currentRemovedSuspension = sum / getPopulation();
		}
		return currentRemovedSuspension;
	}

	public String getCurrentRemovedSuspensionLabel() {
		return "Load removed from suspension by " + name;
	}

	private Double suspendedLoad;

	public double getSuspendedLoad() {
		if (suspendedLoad == null) {
			double sum = 0;
			for (Agent agent : agentList) {
				sum += agent.getSuspendedLoad();
			}
			suspendedLoad = sum / getPopulation();
		}
		return suspendedLoad;
	}

	public String getSuspendedLoadLabel() {
		return "Suspended load of " + name;
	}

	private Double currentNeighborhoodRequest;

	public double getCurrentNeighborhoodRequest() {
		if (currentNeighborhoodRequest == null) {
			double sum = 0.0;
			for (Agent agent : agentList) {
				sum += agent.getCurrentNeighborhoodRequest();
			}
			currentNeighborhoodRequest = sum / getPopulation();
		}
		return currentNeighborhoodRequest;
	}

	public String getCurrentNeighborhoodRequestLabel() {
		return "Current neighborhood requested from " + name;
	}

	private Double meanNeighborhoodRequest;

	public double getMeanNeighborhoodRequest() {
		if (meanNeighborhoodRequest == null) {
			double sum = 0.0;
			for (Agent agent : agentList) {
				sum += agent.getMeanNeighborhoodRequest();
			}
			meanNeighborhoodRequest = sum / getPopulation();
		}
		return meanNeighborhoodRequest;
	}

	public String getMeanNeighborhoodRequestLabel() {
		return "Mean neighborhood requested from " + name;
	}

	private Double currentPowerSoldToNeighbors;

	public double getCurrentPowerSoldToNeighbors() {
		if (currentPowerSoldToNeighbors == null) {
			double sum = 0.0;
			for (Agent agent : agentList) {
				sum += agent.getCurrentPowerSoldToNeighbors();
			}
			currentPowerSoldToNeighbors = sum / getPopulation();
		}
		return currentPowerSoldToNeighbors;
	}

	public String getCurrentPowerSoldToNeighborsLabel() {
		return "Current power sold to neighbors by " + name;
	}

	private Double currentProfit;

	public double getCurrentProfit() {
		if (currentProfit == null) {
			double sum = 0.0;
			for (Agent agent : agentList) {
				sum += agent.getCurrentProfit();
			}
			currentProfit = sum / getPopulation();
		}
		return currentProfit;
	}

	public String getCurrentProfitLabel() {
		return "Current profit of " + name;
	}

	public double getMeanProfit() {
		return profitHistory;
	}

	public String getMeanProfitLabel() {
		return "Mean profit of " + name;
	}
	
	public String toString() {
		return name;
	}
}
