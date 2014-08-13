package power;

// DO NOT FORGET:
// TODO, FIX, ATTENTION, NOTE and bad, poor...

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import power.auction.Auctioneer;
import power.auction.Market;
import power.components.Agent;
import power.components.Grid;
import power.components.PowerPlant;
import power.components.generators.AgentGenerator;
import power.helpers.XmlTools;
import power.networks.MinMaxNetworkGenerator;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.graph.NetworkGenerator;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.graph.Network;

public class SmartGridBuilder implements ContextBuilder<Object> {

	private enum XmlNode {
		GridModel,
		AgentGenerator
	}
	
	private static Context<Object> context;
	private static Network<Object> network;
	private static Grid grid;
	private static HashMap<String, AgentGenerator> agentGeneratorMap;

	// parameters
	private String fileName;
	
	private int minDegree;
	private int maxDegree;
	private int maxNumberOfRounds;
	private boolean isConnected;
	
	@Override
	public Context<Object> build(Context<Object> context) {
		System.out.println();
		System.out.println("Starting new simulation");
		System.out.println("________________________________");
		
		context.setId("Smart Grid");
		SmartGridBuilder.context = context;
		SmartGridBuilder.agentGeneratorMap = new HashMap<String, AgentGenerator>();
		
		readParamaters();
		initialize();
		
		double sum = 0;
		for (AgentGenerator gen:SmartGridBuilder.agentGeneratorMap.values()) {
			List<Agent> list = gen.getAgents();
			for (Agent agent:list) {
				sum += agent.getMeanLoadSeed();
			}
		}
		
		SmartGridBuilder.simulationConstant = sum;
		
		return context;
	}

	private void initialize() {
		Element root = null;
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			root = builder.parse(fileName).getDocumentElement();
		} catch (Exception e) { // poor error handling
			System.err.println("Unable to open or parse: " + fileName);
			(new Exception()).printStackTrace();
			System.exit(1);
		}
		
		SmartGridBuilder.grid = Grid.create(XmlTools.getExactlyOneNode(root, XmlNode.GridModel));
				
		ArrayList<Node> nodeList = XmlTools.getAtLeastOneNode(root, XmlNode.AgentGenerator);
		for (Node node:nodeList) {
			AgentGenerator agentGen = AgentGenerator.create(node);
			SmartGridBuilder.getAgentGeneratorMap().put(agentGen.getName(), agentGen);
			agentGen.createAll();
		}
		
		generateNetwork();
		
		SmartGridBuilder.getContext().add(grid);
		if (grid.getPlantList()!= null) {
			for (PowerPlant plant:grid.getPlantList()) {
				SmartGridBuilder.getContext().add(plant);
			}
		}
		for (AgentGenerator agentGen:SmartGridBuilder.getAgentGeneratorMap().values()) {
			SmartGridBuilder.getContext().add(agentGen);
		}
	}
	
	private void readParamaters() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		fileName = (String) params.getValue("fileName");
		minDegree = (Integer) params.getValue("minDegree");
		maxDegree = (Integer) params.getValue("maxDegree");
		maxNumberOfRounds = (Integer) params.getValue("maxNumberOfRounds");
		isConnected = (Boolean) params.getValue("isConnected");
	}
	
	private void generateNetwork() {
		NetworkBuilder<Object> networkBuilder = new NetworkBuilder<Object>("Smart Grid Network", getContext(), true);
		NetworkGenerator<Object> networkGenerator = new MinMaxNetworkGenerator<Object>(minDegree, maxDegree, maxNumberOfRounds, isConnected);
		networkBuilder.setGenerator(networkGenerator);
		SmartGridBuilder.network = networkBuilder.buildNetwork();
	}

	/*
	 * Getter/Setters
	 */
	public static Context<Object> getContext() {
		return context;
	}

	public static Network<Object> getNetwork() {
		return network;
	}
	
	public static HashMap<String, AgentGenerator> getAgentGeneratorMap() {
		return agentGeneratorMap;
	}
	
	public static int getAgentPopulation() {
		int population = 0;
		for (AgentGenerator agentGen : agentGeneratorMap.values()) {
			population += agentGen.getPopulation();
		}
		return population;
	}

	public static Grid getGrid() {
		return grid;
	}
	
	public static int getPeriod() {
		return (Integer) RunEnvironment.getInstance().getParameters().getValue("period");
	}
	
	public static int getWindowSize() {
		return (Integer) RunEnvironment.getInstance().getParameters().getValue("windowSize");
	}

	public static boolean getCanTrade() {
		return (Boolean) RunEnvironment.getInstance().getParameters().getValue("canTrade");
	}
	
	public static boolean getCanSuspendLoad() {
		return (Boolean) RunEnvironment.getInstance().getParameters().getValue("canSuspendLoad");
	}
	
	public static double getHourlyGridBuyBack() {
		return (Double) RunEnvironment.getInstance().getParameters().getValue("hourlyGridBuyBack");
	}

	public static double getHistoryValue() {
		return (Double) RunEnvironment.getInstance().getParameters().getValue("historyValue");
	}

	public static boolean getSimulateReliability() {
		return (Boolean) RunEnvironment.getInstance().getParameters().getValue("simulateReliability");
	}
	
	public static boolean getPauseOnBlackout() {
		return (Boolean) RunEnvironment.getInstance().getParameters().getValue("pauseOnBlackout");
	}
	
	public static boolean getForceDumping() {
		return (Boolean) RunEnvironment.getInstance().getParameters().getValue("forceDumping");
	}

	private static double simulationConstant;
	public static double getSimulationConstant() {
		return simulationConstant; 
	}
}