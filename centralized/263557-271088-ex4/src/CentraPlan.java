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

    public HashMap<Vehicle, List<ExTask>> changeVehicle(CentraPlan centraPlanOld, Vehicle v1 ,Vehicle v2){
        // pass the first task from v1 to v2
        // will be called only for v1 with nonempty task set
        HashMap<Vehicle, List<ExTask>> Anew = (HashMap<Vehicle, List<ExTask>>) centraPlanOld.A.clone();ExTask tmpPickup = Anew.get(v1).remove(0); // get ExTask object (should be Pickup)
        ExTask tmpDeliver = null;
        for (int i=0; i < Anew.get(v1).size();i++){
            if (Anew.get(v1).get(i).task == tmpPickup.task){
                tmpDeliver = Anew.get(v1).remove(i);
            }
        }
        Anew.get(v2).add(0, tmpDeliver);
        Anew.get(v2).add(0, tmpPickup);
        return Anew;
    }

    public boolean canSwap(CentraPlan centraPlanOld, Vehicle v1, int idx1, int idx2){
        /* criteria to be able to swap: if task(idx1)== pickup: check that it's possible to pickup later
                                            return false
        //                              else if task(idx2)== deliver:  check it's possible to deliver earlier
                                            return false
        /                               return true

        */
        List<ExTask> vehicleActions = centraPlanOld.A.get(v1);

        if (vehicleActions.get(idx1).actionType == ExTask.ActionType.PICKUP) {
            for(int i = idx1+1; i <= idx2; i++){
                if(vehicleActions.get(idx1).task == vehicleActions.get(i).task){
                    return false;
                }
            }
        } else {
            // actionType == DELIVER
            for(int i = idx1; i < idx2; i++){
                if(vehicleActions.get(idx2).task == vehicleActions.get(i).task){
                    return false;
                }
            }
        }
        return true;
    }

    public HashMap<Vehicle, List<ExTask>> swapTask(CentraPlan centraPlanOld, Vehicle v1, int idx1, int idx2){
        HashMap<Vehicle, List<ExTask>> Anew = (HashMap<Vehicle, List<ExTask>>) centraPlanOld.A.clone();

        ExTask tmp = Anew.get(v1).get(idx1); // get ExTask object (should be Pickup)
        Anew.get(v1).set(idx1,Anew.get(v1).get(idx2));
        Anew.get(v1).set(idx2,tmp);
        return Anew;
    }

}
