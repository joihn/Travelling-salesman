package reactive;

import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
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
	private ArrayList<State_Action> stateActionList;
	private HashMap<State_Action, ArrayList<FutureState_Prob>> transitionTable;
	private HashMap<State,ArrayList<String>> actionTable;

	private HashMap<State_Action, Double> rewardTable;

	private HashMap<State, String> actionLookupTable;
	private HashMap<String, City> cityStringLookupTable;
	City finalDestinationForOnlineTravelling=null;
	private int nCities;
	private double discount = 0.0; // will be modified in setup

	private ArrayList<State> states;

	class State {
		public City currentCity;
		public City potentialPackageDest;

		private State(City currCity, City potentialPackageDest_) {
			currentCity = currCity;
			potentialPackageDest = potentialPackageDest_;
		}
	}

	class FutureState_Prob{
		public State futureState;
		public double probability; // probability of the future state

		private FutureState_Prob(State futState, double p){
			futureState = futState;
			probability = p;
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

	private void createTransitionTable(Topology topo) {

		transitionTable = new HashMap<State_Action,ArrayList<FutureState_Prob>>();
		stateActionList = new ArrayList<State_Action>();

		for (State currentState : states) { // loop over all possible initial states
			ArrayList<String> avAction = actionTable.get(currentState); // extract possible actions at initial state
			for (String action : avAction) {
				ArrayList<FutureState_Prob> futureStates_Prob = new ArrayList<FutureState_Prob>();

				State_Action state_action = new State_Action(currentState, action); // loop over all possible actions
				stateActionList.add(state_action);

				for(State nextState : states){ // loop over all possible next states
					// initialize transition probability to 0
					FutureState_Prob futStateProb = new FutureState_Prob(nextState,0);

					// TODO: remove this (until next todo)
					/*
					if (state_action.currentState.currentCity == state_action.currentState.potentialPackageDest){
						System.out.println("WARNING: currentCity equals nextCity in currentState-> should not get here");
					}
					if (nextState.currentCity == nextState.potentialPackageDest){
						System.out.println("WARNING: currentCity equals nextCity in nextState-> should not get here");
					}*/
					// TODO: remove until here

					if (state_action.currentState == nextState){
						// exclude the case where states are identical
						continue;
					}
					if(state_action.action == "pickup"){
						// handle pickup cases

						if(nextState.currentCity == state_action.currentState.potentialPackageDest){
							if(nextState.potentialPackageDest != null){
								// assume you end up at the destination city and after delivery
								// there is directly a task available again
								// TODO check if this is true (seems a bit fishy)
								futStateProb.probability = td.probability(nextState.currentCity,nextState.potentialPackageDest);
							} else {
								// agent ends up at the delivery city but there is no task available after delivery
								// the probability that there is no task in city i equals p = 1- sum_j td.p(i,j)
								double cumsum = 0;
								for(City city : topo.cities()){
									if (city != nextState.currentCity){
										cumsum += td.probability(nextState.currentCity, city);
									}
								}
								futStateProb.probability = 1-cumsum;
								//System.out.println("Inverse probability is "+ futStateProb.probability);
							}
						} else {
							// currentCity of the nextState is not equal to the package destination
							// it is impossible to end up in such a state after delivery
							futStateProb.probability = 0;
						}
					} else {
						// handle move cases
						int nNeighbours;
						if (state_action.currentState.currentCity.hasNeighbor(state_action.currentState.potentialPackageDest)){
							// the destination of the refused but available Task is a neighbour -> move to one of the N-1 Neighbours
							nNeighbours = state_action.currentState.currentCity.neighbors().size()-1;
						} else {
							nNeighbours = state_action.currentState.currentCity.neighbors().size();
						}

						if(nextState.potentialPackageDest != null){
							// in the next state is a package available
							futStateProb.probability = td.probability(nextState.currentCity, nextState.potentialPackageDest)/nNeighbours;
						} else {
							// the next state has no task appearing
							// this happens at a probability p = 1-sum_j td.p(i.j) (compare above)
							// TODO check if this is true (seems a bit fishy)
							double cumsum = 0;
							for(City city : topo.cities()){
								if (city != nextState.currentCity){
									cumsum += td.probability(nextState.currentCity, city);
								}
							}

							futStateProb.probability = (1-cumsum)/nNeighbours;
							//System.out.println("Inverse probability is "+ futStateProb.probability);
						}
					}
					futureStates_Prob.add(futStateProb);


					// TODO remove the code from here on
					/*
					if()
					if 		(state_action.currentState.currentCity==state_action.currentState.potentialPackageDest ||  // destination os the same as current town
							state_action.currentState.currentCity==nextState.currentCity ||                            // staying on the spot is forbiden
							nextState.currentCity==nextState.potentialPackageDest                                    // destination is the same as current town; next state
							//state_action.currentState.potentialPackageDest != nextState.currentCity                  // will implemented forbiding useless move later
							 ) {
						futStateProb.probability = 0.0;
					}
					*/
					/*
					else { //action stuff :D

						if (state_action.action == "pickup") // you pick up
						{              // there is package    														// you end up at destination
							if (state_action.currentState.potentialPackageDest != null && nextState.currentCity == state_action.currentState.potentialPackageDest) {
								futStateProb.probability = 1.0;
							}
						} else { // you don't pickup = you move to EXPLORE :D

														// exploring step must be in neighboorhood		this neighboor has a task available FROM HIM
							if (state_action.currentState.currentCity.neighbors().contains(nextState.currentCity) && nextState.potentialPackageDest != null) {
								//iterate trough neighboor
								// we will do this for ALLL neighboor separately
								futStateProb.probability = td.probability(nextState.currentCity, nextState.potentialPackageDest);
							}

//							goto everyone, not only neigbhoor
//							//this step city  has a task available FROM HIM
//							if (nextState.potentialPackageDest != null) {
//								//iterate trough neighboor
//
//								futStateProb.probability = td.probability(nextState.currentCity, nextState.potentialPackageDest);
//							}

						}
					}
					*/
					// TODO remove code until here
				}
				transitionTable.put(state_action,futureStates_Prob);

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
			//System.out.println("Reward from "+state.currentCity + " to " + state.potentialPackageDest + " is " + reward);
			rewardTable.put(stateAction, reward);
		}
	}


	public void RLA(){
		HashMap<State, Double> V = new HashMap<State,Double>();


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
		System.out.println("========   the discount is : " + discount);

		HashMap<State, Double> V0 = new HashMap<State, Double>(V);
		actionLookupTable = new HashMap<State, String>();

		// TODO: implement goodEnough(V,V0) function here (idea: loop over s: if forall |V(s)-V0(s)|<eps
		while(cnt<niter){
			HashMap<State_Action,Double> Q = new HashMap<State_Action,Double>();
			for(State_Action stateAction : stateActionList){
				Double futureReward = new Double(0);
				// sum over all future states s' to get V(s)
				for(FutureState_Prob futureStateProb : transitionTable.get(stateAction)){
					futureReward += futureStateProb.probability*V.get(futureStateProb.futureState);
				}
				Double totalReward = new Double(rewardTable.get(stateAction)+discount*futureReward);
				Q.put(stateAction, totalReward);

				//System.out.println("Iter" + cnt + " - Reward is " + totalReward );
			}

			// extract best action stored in Q for that state and put it in V
			for(State state : states){
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
		for(State_Action sa : stateActionList){
			System.out.println("State (" + sa.currentState.currentCity + ", " + sa.currentState.potentialPackageDest+")"+
					"-- Optimal Reward is " + V.get(sa.currentState) + " -- Optimal action: " + actionLookupTable.get(sa.currentState));
		}
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
			int cityIdx = (int) Math.random()*currentState.currentCity.neighbors().size();
			City nextCity = currentState.currentCity.neighbors().get(cityIdx);

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
