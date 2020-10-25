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

import java.util.HashMap;
import java.util.List;

public class CentraPlan {
    HashMap<Vehicle, List<exTask>> A;

    //initialisation
    public CentraPlan(List<Vehicle> allVehicles, TaskSet allTasks){
        Vehicle biggestVehicle = pickBiggestVehicle(allVehicles);

        for(Vehicle vehicle : allVehicles){
            if (vehicle != biggestVehicle){
                A.put(vehicle, Plan.EMPTY);
            } else {
                Plan plan = new Plan();
                City currentCity = vehicle.getCurrentCity();
                for (Task task : allTasks){
                    for(City city : currentCity.pathTo(task.pickupCity)){
                        plan.appendMove(city);
                    }
                    plan.appendPickup(task);
                    for(City city : task.pickupCity.pathTo(task.deliveryCity)){
                        plan.appendMove(city);
                    }
                    plan.appendDelivery(task);
                }

                // TODO continue from here A.put(vehicle, exPlan);
            }
        }

    }

    private Vehicle pickBiggestVehicle(List<Vehicle> allVehicles){
        Vehicle biggestVehicle;
        double maxCapacity = 0;
        for(Vehicle vehicle : allVehicles){
            if (maxCapacity<vehicle.capacity()){
                biggestVehicle = vehicle;
            }
        }
        return biggestVehicle;

    }   //marcel
    
    //swapVehicle(Vehicle, List<Vehicle>, centraPlan)   // max
    
    //swapOrder(centraPlan, Vehicle, idx1, idx2)     //max

}
