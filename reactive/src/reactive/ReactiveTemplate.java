package reactive;

import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;

	private int numActions;
	private Agent myAgent;

	private HashMap<State_Action, ArrayList<FutureState_Prob>> transitionTable;
	private HashMap<State,ArrayList<String>> actionTable;

	private HashMap<State_Action, Double> rewardTable;

	private HashMap<State, String> actionLookupTable;

	private int nCities;
	private double discount = 0.9;
	private double mvtSuccessRate = 0.95;

	private ArrayList<State> states;

	class State {
		public City currentCity;
		public City goalCity;

		private State(City currCity, City nexCity) {
			currentCity = currCity;
			goalCity = nexCity;
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

		for (City cityFrom : topo) {
			for (City cityTo : topo) {
				// include the state where cityTo==cityFrom -> N*(N+1)
				State state = new State(cityFrom, cityTo);
				states.add(state);
			}
			State state = new State(cityFrom, null);
			states.add(state);
		}

// create ArrayList of Actions: Each element is a city that
		for (State state : states) {
			ArrayList<String> availableActions = new ArrayList<String>();
			// state has a task
			if (state.goalCity != null) {
				String action = "deliver";
				availableActions.add(action);
			} else { // the agent has no task yet
				for (City neighborCity : state.currentCity.neighbors()) {
					String action = neighborCity.name;
					availableActions.add(action);
				}
				String action = "pickup";
				availableActions.add(action);
			}
			actionTable.put(state, availableActions);

		}
	}

	private void createTransitionTable() {
		for (State currentState : states) { // loop over all possible initial states
			ArrayList<String> avAction = actionTable.get(currentState); // extract possible actions at initial state
			for (String action : avAction) {
				State_Action state_action = new State_Action(currentState, action); // loop over all possible actions

				ArrayList<FutureState_Prob> futureStates_Prob = new ArrayList<FutureState_Prob>();

				for(State nextState : states){ // loop over all possible next states

					if (state_action.currentState != nextState){ // exclude that the agent ends up in the same state
						// initialize transition probability to 0
						FutureState_Prob futStateProb = new FutureState_Prob(nextState,0);


						if (action == "pickup"){
							// pickup case (100% probability)
							if ((state_action.currentState.currentCity == nextState.currentCity)&&
									(nextState.goalCity != null)&&
									(state_action.currentState.goalCity == null)) {

								futStateProb.futureState = nextState;
								futStateProb.probability = 1;
							}

						} else if (state_action.action == "deliver"){
							// deliver a task: reset goal city to null with 100% probability if the movement is correct
							if ((state_action.currentState.currentCity == nextState.currentCity)&&
									(nextState.goalCity == null)&&
									(state_action.currentState.currentCity == state_action.currentState.goalCity)){

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

	public void createReward(TaskDistribution td, Topology tp, Vehicle vehicle){
		for(State state : states){
			Double reward = new Double(0);
			for(String action : actionTable.get(state)){
				if ((action != "pickup") || (action != "deliver")){
					// action is move to some city
					City cityStepTo = getCityFromString(action, tp);

					if(state.goalCity!=null){
						// agent has a task
						reward += td.reward(state.currentCity,cityStepTo ) - state.currentCity.distanceTo(cityStepTo)*vehicle.costPerKm();
					} else {
						// agent moves without task
						reward -= state.currentCity.distanceTo(cityStepTo)*vehicle.costPerKm();
					}
				}
				State_Action stateAction = new State_Action(state, action);
				rewardTable.put(stateAction, reward);
			}
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

	public void RLA(){
		HashMap<State, Double> V = new HashMap<State,Double>();

		HashMap<State_Action,Double> Q = new HashMap<State_Action,Double>();
		// initialization
		for(State state : states){
			V.put(state, new Double(0));
			for(String action : actionTable.get(state)){
				State_Action stateAction = new State_Action(state,action);
				if (rewardTable.get(stateAction)>V.get(state)){
					V.put(state,rewardTable.get(stateAction));
				}
			}
		}

		// optimize
		int niter = 10000;
		int cnt = 0;

		HashMap<State, Double> V0 = new HashMap<State, Double>(V);

		while(cnt<niter){
			for(State state : states){
				for(String action : actionTable.get(state)){
					State_Action stateAction = new State_Action(state,action);
					Double futureReward = new Double(0);
					for(FutureState_Prob futureStateProb : transitionTable.get(stateAction)){
						futureReward += futureStateProb.probability*V.get(futureStateProb.futureState);
					}
					Q.put(stateAction, rewardTable.get(stateAction)+discount*futureReward);
				}
				// extract best action

				Double maxQ = new Double(0);
				for(String action : actionTable.get(state)){
					State_Action stateAction = new State_Action(state,action);
					if(Q.get(stateAction)>maxQ){
						maxQ = Q.get(stateAction);
						actionLookupTable.put(state,stateAction.action);
					}
				}
				V.put(state,maxQ);

			}
			cnt++;

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
		createReward(td, topology, vehicle);
		RLA();
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}




}
