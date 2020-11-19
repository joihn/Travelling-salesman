package AuctionSmart;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CentralPlan {

    HashMap<Vehicle, List<ExTask>> content= new HashMap<Vehicle, List<ExTask>>();
    public boolean isFeasible;

    //initialisation

    public CentralPlan(List<Vehicle> allVehicles, List<Task> allTasks){

        Vehicle biggestVehicle = pickBiggestVehicle(allVehicles);
        isFeasible = true;
        for(Vehicle vehicle : allVehicles){
            if (vehicle != biggestVehicle){
                content.put(vehicle, new ArrayList());
            } else {
                List<ExTask> vehiclePlan = new ArrayList<ExTask>();
                for(Task task : allTasks){
                    if (vehicle.capacity()<task.weight){
                        isFeasible = false;
                    }
                    ExTask exTask = new ExTask(task, ExTask.ActionType.PICKUP);
                    vehiclePlan.add(exTask);
                    exTask = new ExTask(task, ExTask.ActionType.DELIVERY);
                    vehiclePlan.add(exTask);
                }

                this.content.put(vehicle,vehiclePlan);
            }
        }
    }
    public CentralPlan(CentralPlan AInit, Task taskToAdd){   // HOT START !!
        //___________deep copy_______________
        this.isFeasible = AInit.isFeasible;
        this.content = new HashMap<Vehicle,List<ExTask>>();

        for (Vehicle v: AInit.content.keySet()){ // deep copy
            //List<AuctionSmart.ExTask> tempList = new ArrayList<AuctionSmart.ExTask>(AInit.content.get(v));

            this.content.put(v, new ArrayList<ExTask>( AInit.content.get(v)) );
        }
        //___________hot add_______________
        List<Vehicle> allVehicles = new ArrayList<Vehicle>();
        allVehicles.addAll(AInit.content.keySet());
        Vehicle biggestVehicle = pickBiggestVehicle(allVehicles);

        if (biggestVehicle.capacity()<taskToAdd.weight){
            isFeasible = false;
        }


        List<ExTask> biggestVehicleNewPlan = new ArrayList<ExTask>();
        // copy the old plan
        biggestVehicleNewPlan = AInit.content.get(biggestVehicle);

        //add extra task
        ExTask exTask = new ExTask(taskToAdd, ExTask.ActionType.DELIVERY);
        biggestVehicleNewPlan.add(0, exTask);

        exTask = new ExTask(taskToAdd, ExTask.ActionType.PICKUP);
        biggestVehicleNewPlan.add(0, exTask);

        this.content.put(biggestVehicle, biggestVehicleNewPlan);

    }

    public CentralPlan(CentralPlan Aold){   // constructor for deep copying a centralplan
        this.isFeasible = Aold.isFeasible;
        this.content = new HashMap<Vehicle,List<ExTask>>();

        for (Vehicle v: Aold.content.keySet()){ // deep copy
            //List<AuctionSmart.ExTask> tempList = new ArrayList<AuctionSmart.ExTask>(Aold.content.get(v));

            this.content.put(v, new ArrayList<ExTask>( Aold.content.get(v)) );
        }
    }


    public static Vehicle pickBiggestVehicle(List<Vehicle> allVehicles){
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

    public static boolean canChangeVehicle(CentralPlan A, Vehicle v1, Vehicle v2){
        // returns true if the first task from v1 can be passed to v2 (check on capacity of v2 only)

        /* prototype
            task = getFirstTask(v1)
            weight = 0
            canChange = false;
            if (capacity(v2) > task.weight)
                canChange = true;
            return canChange
         */
        ExTask firstTask = A.content.get(v1).get(0); ///// create NULL POINTER !!!!!!!!!¨¨
        boolean canChangeVehicle = false;
        if (firstTask.task.weight<v2.capacity()) {
            canChangeVehicle = true;
        }
        return canChangeVehicle;
    }


    public static CentralPlan changeVehicle(CentralPlan A, Vehicle v1 ,Vehicle v2){
        // pass the first task from v1 to v2
        // will be called only for v1 with nonempty task set
        CentralPlan Anew = new CentralPlan(A);
        ExTask tmpPickup = Anew.content.get(v1).remove(0); // get AuctionSmart.ExTask object (should be Pickup)
        ExTask tmpDeliver = null;
        for (int i=0; i < Anew.content.get(v1).size();i++){
            if (Anew.content.get(v1).get(i).task == tmpPickup.task){
                tmpDeliver = Anew.content.get(v1).remove(i);
            }
        }
        Anew.content.get(v2).add(0, tmpDeliver);
        Anew.content.get(v2).add(0, tmpPickup);
        return Anew;
    }



    public boolean canSwap(CentralPlan A, Vehicle v1, int idx1, int idx2){
        /* criteria to be able to swap: if task(idx1)== pickup: check that it's possible to pickup later
                                            return false
        //                              else if task(idx2)== deliver:  check it's possible to deliver earlier
                                            return false
        /                               return true

        */
        List<ExTask> vehicleActions = A.content.get(v1);

        if (vehicleActions.get(idx1).actionType == ExTask.ActionType.PICKUP) { // check that it's possible to pickup later
            for(int i = idx1+1; i <= idx2; i++){
                if(vehicleActions.get(idx1).task == vehicleActions.get(i).task){
                    //System.out.println("canSwap said : not allowed 1");
                    return false;
                }
            }
        }
        if (vehicleActions.get(idx2).actionType == ExTask.ActionType.DELIVERY) {
            // actionType == DELIVER
            for(int i = idx1; i < idx2; i++){    //check it's possible to deliver earlier
                if(vehicleActions.get(idx2).task == vehicleActions.get(i).task){
                    //System.out.println("canSwap said : not allowed 2");
                    return false;
                }
            }
        }

        //checking that we can swap task and still RESPECT THE WEIGHT
        int weight = 0;

        for(int i=0; i<vehicleActions.size();i++){ // browse trough all the action
            if(i==idx1){  //task is one of those which get swapped
                if (vehicleActions.get(idx2).actionType == ExTask.ActionType.PICKUP){
                    weight += vehicleActions.get(idx2).task.weight;
                } else {
                    weight -= vehicleActions.get(idx2).task.weight;
                }
            } else if (i==idx2) { //task is one of those which get swapped
                if (vehicleActions.get(idx1).actionType == ExTask.ActionType.PICKUP){
                    weight += vehicleActions.get(idx1).task.weight;
                } else {
                    weight -= vehicleActions.get(idx1).task.weight;
                }
            } else { //task is not one of those which get swapped
                if (vehicleActions.get(i).actionType == ExTask.ActionType.PICKUP) {
                    weight += vehicleActions.get(i).task.weight;
                } else {
                    weight -= vehicleActions.get(i).task.weight;
                }

            }
            if (weight > v1.capacity()) {
                //System.out.println("canSwap said : not allowed 3");
                return false;
            }
        }
        //System.out.println("canSwap said :  allowed");
        return true;
    }

    public CentralPlan swapTask(CentralPlan A, Vehicle v1, int idx1, int idx2){
        CentralPlan Anew = new CentralPlan(A);

        ExTask tmp = Anew.content.get(v1).get(idx1); // get AuctionSmart.ExTask object (should be Pickup)
        Anew.content.get(v1).set(idx1,Anew.content.get(v1).get(idx2));
        Anew.content.get(v1).set(idx2,tmp);
        return Anew;
    }




    /*
      Hmap getBestNeighboor(list<hmap> neighboors) {
            minCost=math.Inf
            bestNeigboor=null;

            for n in neighboors{
                cost= computeCost(n)
                If cost<mincost
                    etc…...

            }

        }
    */
    public static CentralPlan getBestNeighbour(List<CentralPlan> neighbours){
        double minCost=Double.POSITIVE_INFINITY;
        CentralPlan bestNeighbour=null;

        Collections.shuffle(neighbours);  /// making sure that if there are multiple eually good neighboor, we pick one at random
        for(CentralPlan n: neighbours){
            double cost=computeCost(n);
            if (cost<minCost){
                minCost=cost;
                bestNeighbour= n;
            }
        }
        return bestNeighbour;
    }

    /*
    Int computeCost(hmap n){
        distance=0
        For all vehicle

            currentCity= vehicle.currentCity
    For all task
                distance+= currentcity.pathTo(task.city)
                currentCity=task.city

            cost=distance*costPerDistance
        Return cost
    }

 */

    public static double computeCost(CentralPlan A){
        double totalCost=0;
        for(Vehicle v:A.content.keySet()){
            double distance=0;
            Topology.City currentCity= v.getCurrentCity();

            for(ExTask t:A.content.get(v)){   //checking what kind of Extask we have to do
                if(t.actionType==ExTask.ActionType.PICKUP){
                    distance+= currentCity.distanceTo(t.task.pickupCity);
                    currentCity= t.task.pickupCity;
                }else{
                    distance+=currentCity.distanceTo(t.task.deliveryCity);
                    currentCity= t.task.deliveryCity;
                }

            }
            totalCost+=distance*v.costPerKm();
        }

        return totalCost;
    }


}
