//the list of imports
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import logist.task.DefaultTaskDistribution;;


public class Auction implements AuctionBehavior{
    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private Random random;
    private Vehicle vehicle;
    private City currentCity;
    private long timeout_setup;
    private long timeout_plan;
    private long timeout_bid;
    private double p;
    private double profitRatio;
    private List<CentralPlan> warmStartListAccept;
    private List<CentralPlan> warmStartListDeny;



    private List<Task> wonTasks;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {


        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
        this.vehicle = agent.vehicles().get(0); //TODO modifying, we dont' want only 1 vehicle !
        this.currentCity = vehicle.homeCity();
        this.p=0.3;
        long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
        this.random = new Random(seed);
        this.wonTasks= new ArrayList<Task>();

        this.timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        this.timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        this.timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
        this.profitRatio=1; // todo: maybe increase to 1.1

        int nScenarios = 5;  // TODO dont hardcode
        int horizon = 4; // TODO don't hardcode

        this.warmStartListAccept = new ArrayList<CentralPlan>();
        this.warmStartListDeny = new ArrayList<CentralPlan>();
        for (int j=0; j<nScenarios;j++){
            List<Task> randomTasks = new ArrayList<Task>();
            do{
                Task newRandomTask = ((DefaultTaskDistribution) distribution).createTask();
                if (newRandomTask.weight < CentralPlan.pickBiggestVehicle(agent.vehicles()).capacity()){
                    randomTasks.add(newRandomTask);
                }
            }while(randomTasks.size()<horizon);
            // TODO maybe avoid inifinite loop
            CentralPlan initialPlan = new CentralPlan(agent.vehicles(),randomTasks);
            this.warmStartListAccept.add(initialPlan);
            this.warmStartListDeny.add(initialPlan);
        }






    }

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
    //        if (winner == agent.id()) {
    //            currentCity = previous.deliveryCity;
    //        }


    /*
        if anyOfTheBid = null:
            pass
            #profitRatio stay the same

        else:
            delta= hisBid-ourBid #positive if we won, neg if we lost
            shoulfHavebidded= ourBid+delta/2  # if <2, risckier behavior
            profitRatio= shouldHavebidded/ourBid *profitRatio

            if profitRatio<1: # we want gain !!!
                profitRatio=1
 */


        if (bids[0]!=null && bids[1]!=null ){ // nobody said  "null"
            double ourBid=bids[this.agent.id()];
            double hisBid=bids[Math.abs(this.agent.id()-1)];
            double delta = hisBid-ourBid ; //positive if we won, neg if we lost
            double couldHaveBidded= ourBid + delta/2;
            this.profitRatio = couldHaveBidded/ourBid * this.profitRatio;

            if (this.profitRatio<1){
                this.profitRatio=1;
            }


        }
        if (winner==agent.id()){ // we won !
                this.wonTasks.add(previous);
                List<CentralPlan> newWarmupList = new ArrayList<CentralPlan>();
                for (CentralPlan warmup : this.warmStartListAccept){
                    newWarmupList.add(new CentralPlan(warmup,previous));
                }
                this.warmStartListAccept = newWarmupList;
        }


    }

    @Override
    public Long askPrice(Task task) { //= compute Bid

        if (vehicle.capacity() < task.weight) //check biggest vihcile instead !
        {
            return null;
        } else {

        }
/*____________________________________________________template____________________________________________________
        long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
        long distanceSum = distanceTask
                + currentCity.distanceUnitsTo(task.pickupCity);
        double marginalCost = Measures.unitsToKM(distanceSum
                * vehicle.costPerKm());

        double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
        double bid = ratio * marginalCost;

        return (long) Math.round(bid);
*/

/* ____________________________________________________pseudo code____________________________________________________
	marginalCostUs = estimateMarginalCost(....)

			#if = 1, then no profit, but no loss
	bid= profitRatio * marginalCostUs
*/



    }




    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
/*____________________________________________________template____________________________________________________
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);


        Plan planVehicle1 = naivePlan(vehicle, tasks);

        List<Plan> plans = new ArrayList<Plan>();
        plans.add(planVehicle1);
        while (plans.size() < vehicles.size())
            plans.add(Plan.EMPTY);

        return plans;
 */


        long time_start = System.currentTimeMillis();
        System.out.println("Probabilty p is " + this.p);
        List<Task> taskList = new ArrayList<Task>();
        for(Task task : tasks){
            taskList.add(task);
        }
        STL solution= new STL(taskList, vehicles, this.timeout_plan, this.p); //TODO maybe a warm start

        List<Plan> plans = solution.reconstructPlan(vehicles);

        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");

        return plans;

    }


}


