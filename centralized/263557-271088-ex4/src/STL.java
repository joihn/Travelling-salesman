import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.TaskSet;
import logist.topology.Topology.City;





public class STL {

    public CentraPlan centraPlan;
    public List<HashMap<Vehicle, List<ExTask>>> centraPlanSet;





    public STL(TaskSet taskSet, List<Vehicle> vehicles) {
        CentraPlan centraPlan = new CentraPlan(vehicles,taskSet);
        if (!centraPlan.isFeasible){
            System.out.println("WARNING: your problem is not feasible");
        }

        // while(goodEnough){

            //centraPlanSet = generateNeighboor()
            //centraPlan = localChoice(centraPlanSet, centraPlan)
//    }
        /*
             for(loop idx1 ){
                for (loop idx2 s.t. idx1 < idx2 ){
                    if(canSwap(A, vehicle, idx1, dix2)){
                        Anew = swap();
                        N.add = Anew;
                }
             }
         */
    }


    //makeInitialPlan()    //marcel

    private List<HashMap<Vehicle, List<ExTask>>> generateNeighbour(HashMap<Vehicle, List<ExTask>> centraPlan){


        List<HashMap<Vehicle, List<ExTask>>> N = new ArrayList<HashMap<Vehicle, List<ExTask>>>();

        return centraPlanSet;
    }

    public List<Plan> convertPlan(List<Vehicle> allVehicles){
        List<Plan> globalPlan = new ArrayList<Plan>();
        for (Vehicle vehicle : allVehicles){

            City currentCity = vehicle.getCurrentCity();
            Plan plan  = new Plan(currentCity);

            for(ExTask extendedTask : centraPlan.A.get(vehicle)){
                for(City city : currentCity.pathTo(extendedTask.task.pickupCity)){
                    plan.appendMove(city);
                }
                plan.appendPickup(extendedTask.task);
                for(City city : extendedTask.task.pickupCity.pathTo(extendedTask.task.deliveryCity)){
                    plan.appendMove(city);
                }
                plan.appendDelivery(extendedTask.task);
            }
            globalPlan.add(plan);
        }
        return globalPlan;
    }




    //localChoice(centraPlanSet, centraPlan)()  //will pick Best Neighboor



    //getOptimalPlan()
}
