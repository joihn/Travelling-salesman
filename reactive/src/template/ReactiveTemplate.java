package template;

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

	private int nCities;
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
								futStateProb.probability = mvtSuccessRate;

							} else if (state_action.currentState.currentCity.hasNeighbor(nextState.currentCity)) {
								// state_action.action is not equal to nextState.current city -> move to another city then intended

								// probability of false movements is nonzero for neighbour cities of current city
								// distribute (1-mvtSuccessRate) uniformly on all neighbors of current state
								double prob = (1-mvtSuccessRate)/state_action.currentState.currentCity.neighbors().size();
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

	public static void





		// TODO create a method that returns the city object given the city name


	// TODO transition matrix



	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;

		createActionTable(topology); // initialize states list and actionTable
		createTransitionTable();
		//nCities = topology.size();

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
