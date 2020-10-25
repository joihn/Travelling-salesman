import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;





public class STL {

    public HashMap<Vehicle, List<exTask>> centraPlan;
    public List<HashMap<Vehicle, List<exTask>>> centraPlanSet;





    public STL(TaskSet taskSet, List<Vehicle> vehicles) {
        // centraPlan=makeInitialPlan
        // while(goodEnough){

            //centraPlanSet = generateNeighboor()
            //centraPlan = localChoice(centraPlanSet, centraPlan)
//    }

    }

    //pickBiggestVehicle()   //marcel
    //makeInitialPlan()    //marcel

    private List<HashMap<Vehicle, List<exTask>>> generateNeighbour(HashMap<Vehicle, List<exTask>> centraPlan){


        List<HashMap<Vehicle, List<exTask>>> N = new ArrayList<HashMap<Vehicle, List<exTask>>>;

        return centraPlanSet;
    }
        //swapVehicle()   // max
        //swapOrder()     //max



    //localChoice(centraPlanSet, centraPlan)()  //will pick Best Neighboor



    //getOptimalPlan()
}
