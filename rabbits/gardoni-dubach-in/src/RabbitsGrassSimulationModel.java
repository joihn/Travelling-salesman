import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.util.SimUtilities;

import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.Sequence;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author Marcel Dubach, Maxime Gardoni
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {
		// main objects of the class
		private Schedule schedule; // schedules actions of the simulation
		private RabbitsGrassSimulationSpace rgSpace; // 2D grids for grass and rabbit distribution in space
		private ArrayList agentList; // list of agent objects

		private DisplaySurface displaySurf; // used to display the 2D grid
		private OpenSequenceGraph amountOfRabbitsAndGrass; // used to plot the grass spread and rabbit population

		// default values for the parameters
		private static final int NUMGRASSES = 100;
		private static final int NUMRABBITS = 10;
		private static final int INITSIZE = 20;
		private static final double INITGRASSGROWTHRATE = 0.1;
		private static final int INITBIRTHTHRESHOLD = 30;
		private static final int AGENTINITENERGY = 19;

		// fields for simulation parameters
		private int gridSize = INITSIZE;
		private int numInitRabbits = NUMRABBITS;
		private int numInitGrass = NUMGRASSES;
		private double grassGrowthRate = INITGRASSGROWTHRATE;
		private int birthThreshold = INITBIRTHTHRESHOLD;
		private int initEnergy = AGENTINITENERGY;


		class rabbitsInSpace implements DataSource, Sequence {
			public Object execute(){
				return new Double(getSValue());
			}

			public double getSValue(){
				return (double) countLivingAgents();
			}
		}

		class grassesInSpace implements DataSource, Sequence {
			public Object execute(){
				return new Double(getSValue());
			}
			public double getSValue(){
				return (double) rgSpace.countGrass()*1e-1;
			}
		}

		public static void main(String[] args) {
			
			System.out.println("Rabbit skeleton");
			SimInit init = new SimInit();
			RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
			// Do "not" modify the following lines of parsing arguments
			if (args.length == 0) // by default, you don't use parameter file nor batch mode 
				init.loadModel(model, "", false);
			else
				init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));
			
		}

		public void setup() {
			rgSpace = null; // cleanup the model space

			agentList = new ArrayList();
			schedule  = new Schedule(1);

			// Tear down display
			if (displaySurf != null){
				displaySurf.dispose();
			}
			displaySurf = null;
			// Tear down plot
			if (amountOfRabbitsAndGrass != null){
				amountOfRabbitsAndGrass.dispose();
			}
			amountOfRabbitsAndGrass = null;

			// create new display
			displaySurf = new DisplaySurface(this, "Rabbit Grass Model Window 1");
			// create new plot
			amountOfRabbitsAndGrass = new OpenSequenceGraph("Amount of Rabbits and Grass in Space",this);

			registerDisplaySurface("Rabbit Grass Model Window 1", displaySurf);
			this.registerMediaProducer("Plot", amountOfRabbitsAndGrass);
		}

		public void begin() {
			buildModel();
			buildSchedule();
			buildDisplay();

			displaySurf.display();
			amountOfRabbitsAndGrass.display();
		}

		public void buildModel(){
			System.out.println("Running Build Model");
			rgSpace = new RabbitsGrassSimulationSpace(gridSize,gridSize);
			rgSpace.spreadGrass(numInitGrass);

			for(int i = 0; i<numInitRabbits;i++){
				addNewAgent();
			}
			for (int i = 0; i < agentList.size();i++){
				RabbitsGrassSimulationAgent rgAgent = (RabbitsGrassSimulationAgent) agentList.get(i);
				System.out.println("Agent at x:"+rgAgent.getX()+", y: "+ rgAgent.getY());
				rgAgent.report();
			}
		}

		public void buildSchedule(){
			System.out.println("Running Build Schedule");

			class RabbitGrassStep extends BasicAction {
				public void execute(){
					SimUtilities.shuffle(agentList);
					for(int i = 0; i < agentList.size(); i++){
						RabbitsGrassSimulationAgent rgAgent = (RabbitsGrassSimulationAgent) agentList.get(i);
						rgAgent.setRabbitGrassSpace(rgSpace);
						rgAgent.step();
						tryEat(rgAgent);
						if (rgAgent.getEnergy()>birthThreshold){
							addNewAgent();
							rgAgent.setEnergy(initEnergy); // decrease the parents energy level to init value
						}

					}

					int deadAgents = reapDeadAgents();
					rgSpace.growGrass(grassGrowthRate); // grass growth

					// todo implement reproduction of rabbits
					displaySurf.updateDisplay();
				}

			}
			schedule.scheduleActionBeginning(0,new RabbitGrassStep());

			// counts living agents (may not be done at any step)
			class RabbitGrassCountLiving extends BasicAction{
				public void execute(){
					countLivingAgents();
				}
			}
			schedule.scheduleActionAtInterval(5, new RabbitGrassCountLiving()); // TODO change interval of execution

			class RabbitGrassUpdateRabbitsInSpace extends BasicAction{
				public void execute(){
					amountOfRabbitsAndGrass.step();
				}
			}

			schedule.scheduleActionAtInterval(5,new RabbitGrassUpdateRabbitsInSpace());
		}

		public void buildDisplay(){
			System.out.println("Running Build Display");

			ColorMap map = new ColorMap();
			for(int i = 1; i<16; i++){
				map.mapColor(i, new Color(0,(int)(i*8+127),0));
			}
			map.mapColor(0,Color.white); // default color
			// TODO check different objects
			Value2DDisplay displayGrass = new Value2DDisplay(rgSpace.getCurrentGrassSpace(),map);

			Object2DDisplay displayAgents = new Object2DDisplay(rgSpace.getCurrentAgentSpace());
			displayAgents.setObjectList(agentList);

			displaySurf.addDisplayableProbeable(displayGrass, "Grass");
			displaySurf.addDisplayableProbeable(displayAgents, "Agents");

			amountOfRabbitsAndGrass.addSequence("Rabbits in Space", new rabbitsInSpace());
			amountOfRabbitsAndGrass.addSequence("Grass in Space (in 1e2)", new grassesInSpace());

		}

		// add a new Agent object to the agentList
		private void addNewAgent(){
			RabbitsGrassSimulationAgent rabbit = new RabbitsGrassSimulationAgent(initEnergy);
			if(rgSpace.addRabbit(rabbit)){
				agentList.add(rabbit);
			}
		}

		// count the number of agents that are alive (used for the diagram)
		private int countLivingAgents(){
			int livingAgents = 0;
			for(int i = 0; i<agentList.size();i++){
				RabbitsGrassSimulationAgent rgAgent = (RabbitsGrassSimulationAgent) agentList.get(i);
				if(rgAgent.getEnergy()>0) {
					livingAgents++;
				}
			}
			System.out.println("Number of living agents is: "+livingAgents);
			return livingAgents;
		}

		private int reapDeadAgents(){
			int count = 0;
			for(int i = (agentList.size()-1);i>=0;i--){ // decrement for removal of agents
				RabbitsGrassSimulationAgent rgAgent = (RabbitsGrassSimulationAgent) agentList.get(i);
				if(rgAgent.getEnergy()<1){
					rgSpace.removeAgentAt(rgAgent.getX(),rgAgent.getY());
					agentList.remove(i);
					count++;
				}
			}
			return count;
		}

		public void tryEat(RabbitsGrassSimulationAgent agent){
			int grassAmount = rgSpace.getGrassAt(agent.getX(),agent.getY());
			if(grassAmount>=0){
				rgSpace.removeGrassAt(agent.getX(),agent.getY()); // removes the grass at the agents position
				agent.eatGrass(grassAmount); // increases the energy of the rabbit
			}
		}


		public String[] getInitParam() {
			// Parameters to be set by users via the Repast UI slider bar
			// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
			String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "InitEnergy"};
			return params;
		}

		public String getName() {
			return "Rabbit Model";
		}

		public Schedule getSchedule() {
			return schedule;
		}

		// getters (required for parameter interface)
		public int getNumInitRabbits(){
			return numInitRabbits;
		}

		public int getNumInitGrass(){
			return numInitGrass;
		}

		public int getGridSize(){
			return gridSize;
		}

		public double getGrassGrowthRate(){
			return grassGrowthRate;
		}

		public int getBirthThreshold(){
			return birthThreshold;
		}

		public int getInitEnergy(){
			return initEnergy;
		}


		// setters (required for parameter setting)
		public void setNumInitRabbits(int nRabbits){
			if (nRabbits>0){
				numInitRabbits = nRabbits;
			}
		}

		public void setGridSize(int size){
			if (size>0){
				gridSize = size;
			}
		}

		public void setNumInitGrass(int nGrasses){
			if(nGrasses >0 ){
				numInitGrass = nGrasses;
			}
		}

		public void setGrassGrowthRate(double ggr){
			if ((ggr>0) && (ggr<1)){
				grassGrowthRate = ggr;
			}
		}

		public void setBirthThreshold(int birthThresh){
			if (birthThresh>0){
				birthThreshold = birthThresh;
			}
		}

		public void setInitEnergy(int energy){
			if (energy>0) {
				initEnergy = energy;
			}
		}

}
