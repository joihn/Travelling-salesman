import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class CentraPlan {
    HashMap<Vehicle, List<ExTask>> A;

    //initialisation
    public CentraPlan(List<Vehicle> allVehicles, TaskSet allTasks){
        Vehicle biggestVehicle = pickBiggestVehicle(allVehicles);

        for(Vehicle vehicle : allVehicles){
            if (vehicle != biggestVehicle){
                A.put(vehicle, new ArrayList());
            } else {
                List<ExTask> vehiclePlan = new ArrayList<ExTask>();
                for(Task task : allTasks){
                    ExTask exTask = new ExTask(task, ExTask.ActionType.PICKUP);
                    vehiclePlan.add(exTask);
                    exTask = new ExTask(task, ExTask.ActionType.DELIVERY);
                    vehiclePlan.add(exTask);
                }
                A.put(vehicle,vehiclePlan);
            }
        }

    }

    private Vehicle pickBiggestVehicle(List<Vehicle> allVehicles){
        Vehicle biggestVehicle = null;
        double maxCapacity = 0;
        for(Vehicle vehicle : allVehicles){
            if (maxCapacity<vehicle.capacity()){
                biggestVehicle = vehicle;
                maxCapacity = vehicle.capacity();
            }
        }
        return biggestVehicle;

    }
    
    //swapVehicle(Vehicle, List<Vehicle>, centraPlan)   // max
    
    //swapOrder(centraPlan, Vehicle, idx1, idx2)     //max

}
