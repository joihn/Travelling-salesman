import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.TaskSet;
import logist.topology.Topology.City;





public class STL {

    public CentralPlan A;
    public CentralPlan Aold;



    public STL(TaskSet taskSet, List<Vehicle> vehicles) {
        /* pseudo code:
            A = initializeA(taskset, vehicleset)

            while(!goodEnough()) // combine cost improvement between A and Aold
                Aold = A;
                N = generateNeighbours(Aold) // generate all neighbors by copy!
                A = localChoice(N,A,p)  // select best neighbour with probability p, else select A
            return A
         */

        // initialization
        A = new CentralPlan(vehicles,taskSet);
        if (!A.isFeasible){
            System.out.println("WARNING: your problem is not feasible");
        }
        while(1) { // TODO implement good enough
            Aold = new CentralPlan(A);
            List<HashMap<Vehicle, List<ExTask>>> N = generateNeighbour();

        }

    }


    //makeInitialPlan()    //marcel

    private List<HashMap<Vehicle, List<ExTask>>> generateNeighbour(CentralPlan Aold, List<Vehicle> allVehicles){
        // generate mutations to find other feasible sequences
        /*
            v1 = selectRandomVehicle(List<Vehicle>) // select random vehicle with tasks
            loop v2:
                if (canChangeVehicle(v1,v2))
                // assume v1 has at least 1 task
                // check on capacity of v2 to take the first task of v1 // TODO
                    A = ChangeVehicle(v1,v2)
                    N.append(A)

            loop(idx1)
                loop(idx2>idx1)
                    if(canSwap(Aold,v1,idx,idx2)
                        A = swap(Aold,v1,idx,idx2)
                        N.append(A)

            return N

         */
        List<HashMap<Vehicle, List<ExTask>>> N = new ArrayList<HashMap<Vehicle, List<ExTask>>>(); // neighbour plans are a list of HashMap


        Vehicle v1 = selectRandomVehicle(allVehicles);
        for(Vehicle v2 : allVehicles){
            if(Aold.canChangeVehicle(Aold.content,v1,v2)){
                HashMap<Vehicle, List<ExTask>> Anew = Aold.changeVehicle(Aold.content, v1, v2);
                N.add(Anew);
            }
        }

        for (int idx1=0; idx1<Aold.content.get(v1).size()-1;idx1++){
            for(int idx2=idx1+1; idx2<Aold.content.get(v1).size();idx2++){
                if(Aold.canSwap(Aold.content,v1,idx1,idx2)){
                    HashMap<Vehicle,List<ExTask>> Anew = Aold.swapTask(Aold.content,v1,idx1,idx2);
                    N.add(Anew);
                }
            }
        }
        return N;
    }

    public List<Plan> convertPlan(List<Vehicle> allVehicles){
        List<Plan> globalPlan = new ArrayList<Plan>();
        for (Vehicle vehicle : allVehicles){

            City currentCity = vehicle.getCurrentCity();
            Plan plan  = new Plan(currentCity);

            for(ExTask extendedTask : A.content.get(vehicle)){
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



    public Vehicle selectRandomVehicle(List<Vehicle> allVehicles){
        // select a vehicle with a nonempty task set
        int nTasks = 0;
        Vehicle vehicle;
        do{
            int randIdx = (int) (Math.random()*allVehicles.size());
            vehicle = allVehicles.get(randIdx);
            nTasks = A.content.get(vehicle).size();
        }while(nTasks==0);
        return vehicle;
    }
    //localChoice(centraPlanSet, centraPlan)()  //will pick Best Neighboor



    //getOptimalPlan()
}
