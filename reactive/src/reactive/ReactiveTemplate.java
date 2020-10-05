package reactive;

import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.FileWriter;
import java.io.IOException;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;



public class ReactiveTemplate implements ReactiveBehavior {
	// TODO add mvtPrecision --> why is this not here anymore?
	private Random random;
	private double pPickup;

	private int numActions;
	private Agent myAgent;
	private TaskDistribution td;

	private ArrayList<State> states; // all possible states of the agent - state (i,i) expluded
	private HashMap<State,ArrayList<String>> actionTable; // returns all possible actions at state s

	private ArrayList<State_Action> stateActionList;

	private HashMap<State_Action, ArrayList<StateActionFutureState>> futureStatesTable; // Lookup table for s'
	private HashMap<StateActionFutureState, Double> transitionProbability;

	private HashMap<State_Action, Double> rewardTable;

	private HashMap<State, String> actionLookupTable;
	private HashMap<String, City> cityStringLookupTable;

	City finalDestinationForOnlineTravelling=null;
	private int nCities;
	private double discount = 0.0; // will be modified in setup

	class State {
		public City currentCity;
		public City potentialPackageDest;

		private State(City currCity, City potentialPackageDest_) {
			currentCity = currCity;
			potentialPackageDest = potentialPackageDest_;
		}
	}

	class StateActionFutureState{
		public State state;
		public String action;
		public State futureState;

		private StateActionFutureState(State s, String a, State fs){
			state = s;
			action = a;
			futureState = fs;
		}
	}

	class State_Action{
		public State currentState;
		public String action;
		/**
		 * @param s 	state object (current location, potentialPackageDestination)
		 * @param act	action is a String corresponding to the name of the city where the agents goes
		 */
		private State_Action(State s, String act) {
			currentState = s;
			action = act;
		}
	}

	public void cityStringLookupTableFiller(Topology topology){
		cityStringLookupTable = new HashMap<String, City>();
		for(City city : topology){

			cityStringLookupTable.put(city.name, city);
		}
	}

	public City getCityFromString(String cityName, Topology tp){
		for(City city : tp){
			if (cityName == city.name){
				return city;
			}
		}
		// action string corresponds to no city Name
		System.out.println("WARNING: action string matches none of the city names");
		return null; // TODO check how to raise an exception
	}

	/** createActionTable creates:
	 * - a list of all possible states
	 * - a list of all possible action at a given state
	 * - stores the result in a HashMap called actionTable
	 **/
	private void createActionTable(Topology topo) {
		// create ArrayList of States
		states = new ArrayList<State>();
		actionTable = new HashMap<State,ArrayList<String>>();

		// create ArrayList of all possible states
		for (City cityFrom : topo) {
			for (City potentialPackageDest : topo) {
				// exclude the case where the Destination equals the current City!
				if (cityFrom != potentialPackageDest){
					State state = new State(cityFrom, potentialPackageDest);
					states.add(state);
				}
			}
			State state = new State(cityFrom, null);
			states.add(state);
		}

		// create ArrayList of possible actions at each possible state
		for (State state : states) {
			ArrayList<String> availableActions = new ArrayList<String>();
			String action = new String();
			if (state.potentialPackageDest != null){
				// at this state a task is available -> add action to take the task
				action = "pickup";
				availableActions.add(action);
			}
			for (City neighborCity : state.currentCity.neighbors()) {
				// if destination is a neighbor it can be reached through action "pickup" already
				if(neighborCity != state.potentialPackageDest){
					action = neighborCity.name;
					availableActions.add(action);
				}
			}
			actionTable.put(state, availableActions);

		}

		stateActionList = new ArrayList<State_Action>();
		for(State state : states){
			for(String action : actionTable.get(state)){
				State_Action stateAction = new State_Action(state, action);
				stateActionList.add(stateAction);

				ArrayList<StateActionFutureState> futureStatesList = new ArrayList<StateActionFutureState>();
				// define possible future states for the couple (s,a)
				for(State futureState : states){
					if((stateAction.currentState.currentCity.hasNeighbor(futureState.currentCity))
							||((futureState.currentCity == stateAction.currentState.potentialPackageDest)&&
							action=="pickup")){
						StateActionFutureState safs = new StateActionFutureState(stateAction.currentState, stateAction.action, futureState);
						futureStatesList.add(safs);
					}
				}
				futureStatesTable.put(stateAction,futureStatesList);
			}
		}


		/* TODO remove that comment for print
		for(State stat : states){
			if (stat.potentialPackageDest != null) {
				System.out.println("State: (" + stat.currentCity.name + ", " + stat.potentialPackageDest.name + ")");
			} else {
				System.out.println("State: (" + stat.currentCity.name + ", null)");
			}
		}
		*/
	}


	private void createTransitionTable(Topology topo, TaskDistribution td) {

		transitionProbability = new HashMap<StateActionFutureState,Double>();

		for(State_Action stateAction : stateActionList){
			for(StateActionFutureState safs : futureStatesTable.get(stateAction)){
				// safs contains all possible (s,a,s') tuples
				double probability;

				if(safs.state.potentialPackageDest == null){
					// no task is available
					if(safs.futureState.potentialPackageDest != null){
						// at the future state a task is available
						probability =  1/safs.state.currentCity.neighbors().size()*td.probability(safs.futureState.currentCity,safs.futureState.potentialPackageDest);
					} else {
						// at the future state there is still no task
						double cumsum = 0;
						for(City destinationCity : topo.cities()){
							cumsum += td.probability(safs.futureState.currentCity, destinationCity);
						}
						probability =  (1-cumsum)/safs.state.currentCity.neighbors().size();
					}
				} else {
					// a task is available
					if(safs.action == "pickup"){
						// a task is available and pickup is performed
						if(safs.futureState.potentialPackageDest != null){
							// at the future state again a task is available
							probability = td.probability(safs.futureState.currentCity, safs.futureState.potentialPackageDest);
						} else {
							// at the future state no task is available
							double cumsum = 0;
							for(City destinationCity : topo.cities()){
								cumsum += td.probability(safs.futureState.currentCity,destinationCity);
							}
							probability = 1-cumsum;
						}
					} else {
						// move
						// a task is available but the agent does refuse it
						if(safs.state.currentCity.hasNeighbor(safs.state.potentialPackageDest)){
							if(safs.futureState.potentialPackageDest != null){
								probability = 1/(safs.state.currentCity.neighbors().size()-1)*td.probability(safs.futureState.currentCity,safs.futureState.potentialPackageDest);
							} else {
								// future state has no task available anymore
								double cumsum = 0;
								for(City destinationCity : topo.cities()){
									cumsum += td.probability(safs.futureState.currentCity,destinationCity);
								}

								probability = 1/(safs.state.currentCity.neighbors().size()-1)*(1-cumsum);
							}
						} else {
							if(safs.futureState.potentialPackageDest != null){
								probability = 1/safs.state.currentCity.neighbors().size()*td.probability(safs.futureState.currentCity,safs.futureState.potentialPackageDest);
							} else {
								double cumsum = 0;
								for(City destinationCity : topo.cities()){
									cumsum += td.probability(safs.futureState.currentCity,destinationCity);
								}
								probability = 1/safs.state.currentCity.neighbors().size()*(1-cumsum);
							}
						}

					}

				}
				transitionProbability.put(safs, probability);

			}
		}

	}

	public void createReward(TaskDistribution td, Topology tp, Vehicle vehicle){
		rewardTable = new HashMap<State_Action, Double>();
		/*for (State_Action stateAction : stateActionList){
			System.out.println("State: " + stateAction.currentState.currentCity.name + ","+stateAction.currentState.potentialPackageDest.name + " - Action " + stateAction.action);
		}*/
		for (State_Action stateAction : stateActionList){
			String action = stateAction.action;
			State state = stateAction.currentState;

			Double reward = new Double(0);
			if (action == "pickup") {
				// reward for agent on delivery (agent can only take action delivery if a taskDestination is defined
				City taskDestination = state.potentialPackageDest;

				if (state.potentialPackageDest != null) {
					// reward = task reward - cost of delivery
					reward = td.reward(state.currentCity, taskDestination) - state.currentCity.distanceTo(taskDestination) * vehicle.costPerKm();
				} else {
					// if potentialPackageDest is null, pickup cannot be performed (code should never get here!)
					System.out.println("WARNING: 'pickup' should be impossible at state with undefined Destination");
				}
			} else {
				// reward is negative if no task is taken and the vehicle moves arount empty
				reward = - state.currentCity.distanceTo(getCityFromString(action, tp))*vehicle.costPerKm();
			}
			System.out.println("Reward from ("+state.currentCity + " to " + state.potentialPackageDest + "), action : " + action + " is " + reward);
			rewardTable.put(stateAction, reward);
		}
	}


	public void RLA(){
		HashMap<State, Double> V = new HashMap<State,Double>();
		/*
		for(State_Action sa : stateActionList){
			System.out.println("State: (" + sa.currentState.currentCity + "," + sa.currentState.potentialPackageDest + ") action: " + sa.action + " -- direct reward: " + rewardTable.get(sa));
		}
		*/
		// initialization of V(s) at 0
		for(State state : states){
			// initialize V with some random action in that state
			V.put(state, new Double(0));
		}

		// loop over all (state,action) pairs, and if R(s,a) > V(s), then V(s) = R(s,a)
		for(State_Action stateAction : stateActionList){
			if (rewardTable.get(stateAction)>V.get(stateAction.currentState)){
				V.put(stateAction.currentState, rewardTable.get(stateAction));
			}
		}
		/*
		System.out.print("After Initialization:");
		for(State_Action sa : stateActionList){
			System.out.println("Optimal Reward is " + V.get(sa.currentState));
		}

		 */

		// optimize
		int niter = 100;
		int cnt = 0;
		discount = 0.9;
		System.out.println("========   the discount is : " + discount);

		HashMap<State, Double> V0 = new HashMap<State, Double>(V);
		actionLookupTable = new HashMap<State, String>();

		// TODO: implement goodEnough(V,V0) function here (idea: loop over s: if forall |V(s)-V0(s)|<eps
		while(cnt<niter){

			for(State state : states){
				// consider a given state and loop over all possible actions of that state

				HashMap<State_Action,Double> Q = new HashMap<State_Action,Double>();

				Double futureReward = new Double(0);
				for(State_Action stateAction : stateActionList){
					if (stateAction.currentState != state){
						continue;
					} else {
						// reward for a given state and a given action -> loop over future states
						for(FutureState_Prob futureStateProbability : transitionTable.get(stateAction)){
							futureReward += futureStateProbability.probability*V.get(futureStateProbability.futureState);
						}
						Double totalReward = new Double(rewardTable.get(stateAction)+discount*futureReward);
						Q.put(stateAction, totalReward);
						//System.out.println("Iter" + cnt + " - Reward is " + totalReward );
					}
				}

				// extract the best possible action
				Double maxQ = new Double(0);
				for(State_Action stateAction: stateActionList){
					if (stateAction.currentState != state){
						continue;
					} else {
						if(Q.get(stateAction) > maxQ){
							// the action in stateAction is the best of all actions so far
							actionLookupTable.put(state,stateAction.action);
							maxQ = Q.get(stateAction);
							V.put(state,maxQ);
						}
					}
				}

			}

			//TODO update V0 (values only!)
			cnt++;

		}

		System.out.print("Finished Optimization");
		/*for(State_Action sa : stateActionList){
			System.out.println("State (" + sa.currentState.currentCity + ", " + sa.currentState.potentialPackageDest+")"+
					"-- Optimal Reward is " + V.get(sa.currentState) + " -- Optimal action: " + actionLookupTable.get(sa.currentState));
		}
		*/

		// TODO: the agent chooses only at 2 states to pickup :S


	}

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount; // TODO can proabbly delete this
		this.numActions = 0;
		this.myAgent = agent;
		this.td=td;


		Vehicle vehicle = agent.vehicles().get(0);
		if(agent.vehicles().size()>1){
			System.out.println("WARNING: Agent has more than 1 vehicle");
		}

		createActionTable(topology); // initialize states list and actionTable
		createTransitionTable(topology);
		createReward(td, topology, vehicle);
		RLA();
		//fill up the cityStringLookupTable for us in "act"
		cityStringLookupTableFiller(topology);
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action=null;

		City stateDestination;
		if (availableTask != null){
			stateDestination = availableTask.deliveryCity;
		} else {
			stateDestination = null;
		}

		// get the pointer to state object (currentCity, destinationCity)
		State currentState = new State(null, null);
		for(State stateIter : states){
			if ((stateIter.currentCity == vehicle.getCurrentCity())&&(stateIter.potentialPackageDest == stateDestination)){
				currentState = stateIter;
			}
		}
		String currentBestAction = actionLookupTable.get(currentState);

		if (currentBestAction == "pickup") {
			System.out.println("Action " + numActions + " is pickup");
			action = new Action.Pickup(availableTask);

		} else if (currentBestAction!=null){
			// move to a RANDOM neighbour
			int cityIdx = (int) (Math.random()*currentState.currentCity.neighbors().size());
			System.out.println("City Index is: " +cityIdx);
			City nextCity = currentState.currentCity.neighbors().get(cityIdx);

			// TODO move to random neighour

			action = new Action.Move(nextCity);
			System.out.println("Action " + numActions + "is Move To" + nextCity.name);

		}else{ // action is null !! :( gotta do something instead !
			City choosenNeighboor=vehicle.getCurrentCity().neighbors().get(0);
			System.out.println("WARINGN POLICY DIDN'T FIND ANYTHING ");
			action= new Action.Move(choosenNeighboor);
		}

		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+ myAgent.getTotalProfit()+
					" (average profit: "+(myAgent.getTotalProfit() / (double) numActions) + ")");
		}

		numActions++;
		return action;


		/*
		if (finalDestinationForOnlineTravelling==null) { // TODO ??
			// identify the current state

			// info avialble for this task :
			// current city
			// if there is an available task
			//    if yes, the destination a this task
			City destination;
			if (availableTask != null) {
				destination = availableTask.deliveryCity;
			} else {
				destination = null; // if no package available, destination is null
			}

			State currentState = new State(vehicle.getCurrentCity(), null);
			// TODO why not put State(vehicle.getCurrentCity(),destination) here??
			int i = 0;
			for (State currentStateIter : states) { // loop over all possible initial states

				if (currentStateIter.currentCity == vehicle.getCurrentCity() && destination == currentStateIter.potentialPackageDest) {
					currentState = currentStateIter;
					i++;
				}

			}
			if (i != 1) {
				System.out.println("WARING, didn't find the correct amount of current states, it should be 1");
			}

			if (numActions >= 1) {
				System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
			}

			//System.out.println("Currently acting");

			String currentBestAction = actionLookupTable.get(currentState); //TODO remove error

			if (currentBestAction == "pickup") {
				//System.out.println("will try to pickup");
				action = new Action.Pickup(availableTask);
				//finalDestinationForOnlineTravelling = availableTask.deliveryCity; // will go to delivery
			} else if (currentBestAction!=null){ // action is a city
				//System.out.println("will try to move");
				City goalCity = cityStringLookupTable.get(currentBestAction);

				if (vehicle.getCurrentCity().neighbors().contains(goalCity)) {
					action = new Action.Move(goalCity);
					System.out.println("neighboor was my final destination HEHEHE! ");
				} else {
					finalDestinationForOnlineTravelling = goalCity;
					new Action.Move(vehicle.getCurrentCity().pathTo(goalCity).get(0));
				}

			}else{ // action is null !! :( gotta do something instead !
				City choosenNeighboor=vehicle.getCurrentCity().neighbors().get(0); //TODO get closed city instead of first
				System.out.println("WARINGN POLICY DIDN'T FIND ANYTHING ");

				action= new Action.Move(choosenNeighboor);
			}
		}else{ // travelling along the way for the final destination
			City next_step = vehicle.getCurrentCity().pathTo(finalDestinationForOnlineTravelling).get(0);
			action = new Action.Move(next_step);
			if (next_step==finalDestinationForOnlineTravelling){
				finalDestinationForOnlineTravelling=null;
			}
		}

		//logging to file
		String currentCountry="switzerland";
		try{
			FileWriter logger = null;
			logger = new FileWriter("perfLog/"+currentCountry+".csv", true);
			Double rewardPerKm = ((double) myAgent.getTotalReward() / (double)myAgent.getTotalDistance());
			logger.append(myAgent.name()+ ";" + numActions+ ";" + myAgent.getTotalReward() + ";" + myAgent.getTotalDistance() +";" + rewardPerKm +"\n");
			logger.flush();
			logger.close();
		}catch(IOException theError){
			System.out.println("error writing log !");
			theError.printStackTrace();
		}


		numActions++;
		return action;

		 */
	}




}
