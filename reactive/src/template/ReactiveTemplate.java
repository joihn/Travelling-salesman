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

	private HashMap<State_Action, FutureState_Prob> transitionTable;
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
		public State state;
		public String action;

		private State_Action(State s, String act) {
			state = s;
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
		for (State currentState : states) {
			ArrayList<String> avAction = actionTable.get(currentState);
			for (String action : avAction) {
				State_Action state_action = new State_Action(currentState, action);
				for(State nextState : states){
					if (currentState != nextState){ // exclude that the agent ends up in the same state

						// initialize transition probability to 0
						FutureState_Prob futStateProb = new FutureState_Prob(nextState,0);

						// move to the next city (no city has name deliver or pickup)
						if (state_action.action == nextState.currentCity.name){
							futStateProb.futureState = nextState;
							futStateProb.probability = mvtSuccessRate;
							transitionTable.put(state_action,futStateProb);

						// probability of the next state being not the desired one is nonzero
						} else if (currentState.currentCity.hasNeighbor(nextState.currentCity)) {
								// distribute (1-mvtSuccessRate) uniformly on all neighbors of current state
								double prob = (1-mvtSuccessRate)/currentState.currentCity.neighbors().size();
								futStateProb.futureState = nextState;
								futStateProb.probability = prob;
						}
						// implement pickup with 100% success
						if ((currentState.goalCity == null)&&(action == "pickup")){ // TODO discuss if this is 100%
							futStateProb.futureState = nextState;
							futStateProb.probability = 1.;
						// implement deliver with 100% success
						} else if (currentState.goalCity == nextState.currentCity) { // if delivery possible -> deliver
							futStateProb.futureState = nextState;
							futStateProb.probability = 1.;
						}
					}
				}
			}
		}
	}





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
