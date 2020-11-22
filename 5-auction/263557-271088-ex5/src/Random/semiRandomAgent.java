package Random;



//the list of imports

import logist.LogistSettings;
import logist.Measures;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import AuctionSmart.ExTask;
import AuctionSmart.STL;
import AuctionSmart.CentralPlan;


/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class semiRandomAgent implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private long timeout_setup;
	private long timeout_plan;
	private long timeout_bid;


	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
		}
		catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();

		long seed = -9019544669489963951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
		this.timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		// the plan method cannot execute more than timeout_plan milliseconds
		this.timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
		this.timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			currentCity = previous.deliveryCity;
		}
	}

	@Override
	public Long askPrice(Task task) {

		if (vehicle.capacity() < task.weight)
			return null;

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask
				+ currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum
				* vehicle.costPerKm());

		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid = ratio * marginalCost;

		return (long) Math.round(bid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

		// Generate a centralized plan for all obtained tasks
		long time_start = System.currentTimeMillis();
		System.out.println("Probabilty p is " + 0.3);
		List<Task> taskList = new ArrayList<Task>();
		for(Task task : tasks){
			taskList.add(task);
		}
		STL solution= new STL(taskList, vehicles, this.timeout_plan, 0.3);

		List<Plan> plans = solution.reconstructPlan(vehicles);

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("Delivery plan was generated in " + duration + " milliseconds.");

		return plans;

	}


}
