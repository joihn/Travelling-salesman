package reactive;

import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;

	private int numActions;
	private Agent myAgent;

	private ArrayList<State_Action> stateActionList;
	private HashMap<State_Action, ArrayList<FutureState_Prob>> transitionTable;
	private HashMap<State,ArrayList<String>> actionTable;

	private HashMap<State_Action, Double> rewardTable;

	private HashMap<State, String> actionLookupTable;
	private HashMap<String, City> cityStringLookupTable;
	City finalDestinationForOnlineTravelling=null;
	private int nCities;
	private double discount = 0.9;
	private double mvtSuccessRate = 0.95;

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

		private State_Action(State s, String act) {
			currentState = s;
			action = act;
		}
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
		for (City cityFrom : topo) {
			for (City potentialPackageDest : topo) {
				// include the state where cityTo==cityFrom -> N*(N+1)
				State state = new State(cityFrom, potentialPackageDest);
				states.add(state);
				//System.out.println("Added state (" + state.currentCity + ", " + state.goalCity+")");
			}
			State state = new State(cityFrom, null);
			states.add(state);

			//System.out.println("Added state (" + state.currentCity + ", " + state.goalCity+")");
		}

// create ArrayList of Actions: Each element is a city that

		for (State state : states) {
			ArrayList<String> availableActions = new ArrayList<String>();
			// state has a task
			String action = new String();
			// move to neighbour is always possible (even during a task)
			for (City neighborCity : state.currentCity.neighbors()) {
				action = neighborCity.name;
				availableActions.add(action);
			}

			if (state.potentialPackageDest != null) {
				// agent has a task
				if (state.potentialPackageDest == state.currentCity){
					action = "deliver";
				} else {
					action = state.potentialPackageDest.name;
				}
			} else {
				// the agent has no task yet
				action = "pickup";
			}
			availableActions.add(action);

//			for (String act : availableActions){
//				System.out.println("State: ("+state.currentCity+","+state.goalCity + ") action: " + act);
//			}

			actionTable.put(state, availableActions);

		}
	}

	private void createTransitionTable() {

		transitionTable = new HashMap<State_Action,ArrayList<FutureState_Prob>>();
		stateActionList = new ArrayList<State_Action>();

		for (State currentState : states) { // loop over all possible initial states
			ArrayList<String> avAction = actionTable.get(currentState); // extract possible actions at initial state
			for (String action : avAction) {
				State_Action state_action = new State_Action(currentState, action); // loop over all possible actions
				stateActionList.add(state_action);

				ArrayList<FutureState_Prob> futureStates_Prob = new ArrayList<FutureState_Prob>();

				for(State nextState : states){ // loop over all possible next states

					if (state_action.currentState != nextState){ // exclude that the agent ends up in the same state
						// initialize transition probability to 0
						FutureState_Prob futStateProb = new FutureState_Prob(nextState,0);

						if (action == "pickup"){
							// pickup case (100% probability)
							if ((state_action.currentState.currentCity == nextState.currentCity)&&
									(nextState.potentialPackageDest != null)&&
									(state_action.currentState.potentialPackageDest == null)) {

								futStateProb.futureState = nextState;
								futStateProb.probability = 1;
							}

						} else if (state_action.action == "deliver"){
							// deliver a task: reset goal city to null with 100% probability if the movement is correct
							if ((state_action.currentState.currentCity == nextState.currentCity)&&
									(nextState.potentialPackageDest == null)&&
									(state_action.currentState.currentCity == state_action.currentState.potentialPackageDest)){

								futStateProb.futureState = nextState;
								futStateProb.probability = 1;
							}

						} else {
							// action is move to another city
							if (state_action.action == nextState.currentCity.name){
								// probability of correct movements
								futStateProb.futureState = nextState;
								if (currentState.currentCity.neighbors().size() == 1){
									futStateProb.probability = 1;
								} else {
									futStateProb.probability = mvtSuccessRate;
								}

							} else if (state_action.currentState.currentCity.hasNeighbor(nextState.currentCity)) {
								// state_action.action is not equal to nextState.current city -> move to another city then intended

								// probability of false movements is nonzero for neighbour cities of current city
								// distribute (1-mvtSuccessRate) uniformly on all neighbors of current state
								// TODO: check that no division by 0 occurs here
								double prob = (1-mvtSuccessRate)/(state_action.currentState.currentCity.neighbors().size()-1);
								futStateProb.futureState = nextState;
								futStateProb.probability = prob;
							}
						}

						futureStates_Prob.add(futStateProb);
					}
					transitionTable.put(state_action,futureStates_Prob);
				}
			}
		}
	}

	public HashMap<State_Action, Double> createReward(TaskDistribution td, Topology tp, Vehicle vehicle){
		rewardTable = new HashMap<State_Action, Double>();

		//for(State state : states){

		//	for(String action : actionTable.get(state)){
		for (State_Action stateAction : stateActionList){
				String action = stateAction.action;
				State state = stateAction.currentState;

				Double reward = new Double(0);
				if ((action != "pickup") && (action != "deliver")){
					// action is move to some city
					City cityStepTo = getCityFromString(action, tp);

					if(state.potentialPackageDest !=null){
						// agent has a task
						reward += td.reward(state.currentCity,cityStepTo ) - state.currentCity.distanceTo(cityStepTo)*vehicle.costPerKm();
					} else {
						// agent moves without task
						reward -= state.currentCity.distanceTo(cityStepTo)*vehicle.costPerKm();
					}
				}

				rewardTable.put(stateAction, reward);
		}

		return rewardTable;
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

	public void RLA(){
		HashMap<State, Double> V = new HashMap<State,Double>();

		HashMap<State_Action,Double> Q = new HashMap<State_Action,Double>();


		// initialization
		//for(State state : states){
		//	V.put(state, new Double(0));
		for(State_Action stateAction : stateActionList){
			V.put(stateAction.currentState, new Double(0));

			for(String action : actionTable.get(stateAction.currentState)){

				if (rewardTable.get(stateAction)>V.get(stateAction.currentState)){
					V.put(stateAction.currentState,rewardTable.get(stateAction));
				}
			}
		}

		// optimize
		int niter = 10000;
		int cnt = 0;

		HashMap<State, Double> V0 = new HashMap<State, Double>(V);
		actionLookupTable = new HashMap<State, String>();

		while(cnt<niter){
			for(State_Action stateAction : stateActionList){
				Double futureReward = new Double(0);
				for(FutureState_Prob futureStateProb : transitionTable.get(stateAction)){
					futureReward += futureStateProb.probability*V.get(futureStateProb.futureState);
				}
				Q.put(stateAction, rewardTable.get(stateAction)+discount*futureReward);
			}
			// extract best action
			for(State_Action stateAction : stateActionList){
				Double maxQ = new Double(0);
				if(Q.get(stateAction)>maxQ){
					maxQ = Q.get(stateAction);
					actionLookupTable.put(stateAction.currentState,stateAction.action);
					V.put(stateAction.currentState,maxQ);
				}
			}
			cnt++;

		}
		System.out.print("Finished Optimization");

	}
	public void cityStringLookupTableFiller(Topology topology){
		cityStringLookupTable = new HashMap<String, City>();
		for(City city : topology){

			cityStringLookupTable.put(city.name, city);
		}
	}
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount; // TODO pay attention that there are not 2 discount variables
		this.numActions = 0;
		this.myAgent = agent;



		Vehicle vehicle = agent.vehicles().get(0);
		if(agent.vehicles().size()>1){
			System.out.println("WARNING: Agent has more than 1 vehicle");
		}

		createActionTable(topology); // initialize states list and actionTable
		createTransitionTable();
		createReward(td, topology, vehicle); // TODO
		RLA();
		//fill up the cityStringLookupTable for us in "act"
		cityStringLookupTableFiller(topology);
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action=null;

		if (finalDestinationForOnlineTravelling==null) {
			// identify the current state
			// info :
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
			int i = 0;
			for (State currentStateIter : states) { // loop over all possible initial states

				if (currentStateIter.currentCity == vehicle.getCurrentCity() && destination == currentStateIter.potentialPackageDest) {
					currentState = currentStateIter;
					i++;
				}

			}
			if (i != 1) {
				System.out.println("WARING, didn't find the correct amount of current states");
			}

			if (numActions >= 1) {
				System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
			}
			numActions++;
			//System.out.println("Currently acting");

			String currentBestAction = actionLookupTable.get(currentState); //TODO remove error

			if (currentBestAction == "pickup") {
				System.out.println("will try to pickup");
				action = new Action.Pickup(availableTask);
			} else if (currentBestAction == "deliver") {
				Task theCarriedTask = null;
				System.out.println("will try to FAAAAKE deliver package");
				//			for(Task j: vehicle.getCurrentTasks()) {
				//				theCarriedTask = j;
				//				break;
				//			}
				//			action= new Action.Delivery(theCarriedTask);
			} else if (action!=null){ // action is a city
				System.out.println("will try to move");
				City goalCity = cityStringLookupTable.get(currentBestAction);

				if (vehicle.getCurrentCity().neighbors().contains(goalCity)) {
					action = new Action.Move(goalCity);
					System.out.println("neighboor was my final destination HEHEHE! ");
				} else {
					finalDestinationForOnlineTravelling = goalCity; //TODO set to null
					new Action.Move(vehicle.getCurrentCity().pathTo(goalCity).get(0));
				}

			}else{ // action is null !! :( gotta fix that
				City choosenNeighboor=vehicle.getCurrentCity().neighbors().get(0); //TODO get closed one instead of first
				action= new Action.Move(choosenNeighboor);
			}
		}else{ // travelling along the way for the final destination
			City next_step = vehicle.getCurrentCity().pathTo(finalDestinationForOnlineTravelling).get(0);
			action = new Action.Move(next_step);
			if (next_step==finalDestinationForOnlineTravelling){
				finalDestinationForOnlineTravelling=null;
			}
		}



		return action;
	}


}
