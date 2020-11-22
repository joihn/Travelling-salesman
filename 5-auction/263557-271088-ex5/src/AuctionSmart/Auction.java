package AuctionSmart;//the list of imports
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

import logist.task.DefaultTaskDistribution;;


public class Auction implements AuctionBehavior{
    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private Random random;

    private long timeout_setup;
    private long timeout_plan;
    private long timeout_bid;
    private double p;
    private double profitMargin;
    private List<CentralPlan> warmStartListAcceptOld;
    private List<CentralPlan> warmStartListDenyOld;
    private List<CentralPlan> warmStartList;
    private int nScenarios;
    private int horizon;
    private int timePerGlobalScenario;
    private List<Task> wonTasks;

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

        this.p=0.3;  // exploration parameter for centralized planning
        long seed = -9019554669489983951L * agent.vehicles().get(0).homeCity().hashCode() * agent.id();
        this.random = new Random(seed);
        this.wonTasks= new ArrayList<Task>();

        this.timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        this.timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        this.timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
        this.profitMargin =0; // TODO grid search
        //System.out.println("Timeout bid is: " + this.timeout_bid);
        this.timePerGlobalScenario=5*2*1000;
        this.nScenarios = (int) (timeout_bid/(this.timePerGlobalScenario));  // TODO grid search

        System.out.println("n scenario: "+ this.nScenarios);

        this.horizon = 4; // TODO grid search

        this.warmStartListAcceptOld = new ArrayList<CentralPlan>();
        this.warmStartListDenyOld = new ArrayList<CentralPlan>();
        this.warmStartList = new ArrayList<CentralPlan>();






        //initilase list of potential future (nScenria of length horizon)
        for (int j=0; j<nScenarios;j++){
            List<Task> randomTasks = new ArrayList<Task>();

            do{
                Task newRandomTask = ((DefaultTaskDistribution) distribution).createTask();
                if (newRandomTask.weight < CentralPlan.pickBiggestVehicle(agent.vehicles()).capacity()){
                    randomTasks.add(newRandomTask);
                }


            }while(randomTasks.size()<horizon);

            CentralPlan initialPlan = new CentralPlan(agent.vehicles(),randomTasks);
            this.warmStartListAcceptOld.add(initialPlan);
            this.warmStartListDenyOld.add(initialPlan);
            this.warmStartList.add(initialPlan);
        }



    }

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {

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

        // Display result of the auction
        System.out.println("Auction result for task: "+ previous.id);
        if (winner == this.agent.id()){
            System.out.println("                             we WON +++++");
            System.out.println("                              us:"+ bids[this.agent.id()]);
            System.out.println("                              opp:" + bids[Math.abs(this.agent.id()-1)]);


        }else{
            System.out.println("we LOST ----");
            System.out.println("us :"+ bids[this.agent.id()]);
            System.out.println("opp :" + bids[Math.abs(this.agent.id()-1)]);
        }
        System.out.println("-----------------------------------------------------------------------------------------");

        // update the profit margin
        if (bids[0]!=null && bids[1]!=null ){ // nobody said  "null"
            double ourBid=bids[this.agent.id()];
            double oppBid=bids[Math.abs(this.agent.id()-1)];

            double delta = oppBid-ourBid; // delta > 0 if task is won, delta < 0 otherwise

            this.profitMargin += delta*0.1;
            // increase profit margin

            // assure that profit margin is nonnegative
            if (this.profitMargin <0){
                this.profitMargin = 0;
            }
        }

        // update warm start lists
        if (winner==agent.id()){ // we wo
                this.wonTasks.add(previous);
                this.warmStartList=this.warmStartListAcceptOld;
        }else{
            // TODO double check if this is necessary...
            this.warmStartList=this.warmStartListDenyOld;
        }

    }

    @Override
    public Long askPrice(Task task) { //= compute Bid

        Vehicle biggestVehicle = CentralPlan.pickBiggestVehicle(this.agent.vehicles());

        if (biggestVehicle.capacity() < task.weight)
        {
            return null;
        } else {
            long time_start = System.currentTimeMillis();
            double marginalCost= estimateMarginalCost(task);
            System.out.println("                                                             marginalCost: "+ marginalCost);
            System.out.println("                                                             profitMargin: "+ this.profitMargin);
            long bid = (long) (marginalCost+this.profitMargin);

            long time_end = System.currentTimeMillis();

            System.out.println("                                                                                                             nScenario: " + this.nScenarios + "; horizon: " + this.horizon    +"; time: " + (time_end - time_start)/1000 );
            return bid;
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

    private double estimateMarginalCost(Task task) {

        List<STL> scenarioList = new ArrayList<STL>();
        System.out.println("this.timePerGlobalScenario :"+ this.timePerGlobalScenario);
        double marginalCost;
        List<Double> marginalCostList = new ArrayList<Double>();
        for (int i=0; i<this.warmStartList.size(); i++){
            CentralPlan scenario = this.warmStartList.get(i);

            int safetyTime = 100;
            double wAccept=0.0;
            double wDeny=0.0;
            if (this.wonTasks.size()==0){ // if init, comput time is split in half
                wAccept=0.5;
                wDeny=0.5;
            }else{
                wAccept=0.8;
                wDeny=0.2;      // we already computed a good old optimization before
            }

            STL scenarioDeny = new STL(this.agent.vehicles(), (long)(wDeny*this.timePerGlobalScenario - safetyTime), this.p, scenario, null);
            this.warmStartListDenyOld.set(i,scenarioDeny.bestASoFar);

            STL scenarioAccept = new STL(this.agent.vehicles(),(long)(wAccept*this.timePerGlobalScenario -safetyTime), this.p, scenario, task);
            this.warmStartListAcceptOld.set(i,scenarioAccept.bestASoFar);

            marginalCost = (Math.max(scenarioAccept.bestCostSoFar-scenarioDeny.bestCostSoFar,0));
            if ((scenarioAccept.bestCostSoFar-scenarioDeny.bestCostSoFar)<0){
                System.out.println("the marginal cost is neg !!!! "); //todo check this REALLY !!!!!
            }
            marginalCostList.add(marginalCost);
        }

        // calculate the mean marginal cost
        double mean=0;
        for (double mc: marginalCostList){
            mean += mc/marginalCostList.size();
        }
        return mean; // 0*std(marginalCostList, mean); // TODO remove this
    }



    // TODO std is probably not needed
    /*
    public static double std (List<Double> table, double mean)
    {
        // Step 1:
        double temp = 0;

        for (int i = 0; i < table.size(); i++)
        {
            double val = table.get(i);

            // Step 2:
            double squrDiffToMean = Math.pow(val - mean, 2);

            // Step 3:
            temp += squrDiffToMean;
        }

        // Step 4:
        double meanOfDiffs = (double) temp / (double) (table.size() -1 );

        // Step 5:
        return Math.sqrt(meanOfDiffs);
    }
    */


    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        // Generate a centralized plan for all obtained tasks
        long time_start = System.currentTimeMillis();
        System.out.println("Probabilty p is " + this.p);
        List<Task> taskList = new ArrayList<Task>();
        for(Task task : tasks){
            taskList.add(task);
        }
        STL solution= new STL(taskList, vehicles, this.timeout_plan, this.p);

        List<Plan> plans = solution.reconstructPlan(vehicles);

        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("Delivery plan was generated in " + duration + " milliseconds.");

        return plans;

    }


}


