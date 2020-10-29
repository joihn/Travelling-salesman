import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;
import org.jdom.IllegalNameException;


public class STL {

    public CentralPlan A ;
    public CentralPlan Aold;
    public double p =0.4;  //TODO should we harcode this ?????????????????????????????????????????????????????????????
    public int iterWithNoChange=0;

    public STL(TaskSet taskSet, List<Vehicle> allVehicles) {
        /* pseudo code:
            A = initializeA(taskset, vehicleset)

            while(!goodEnough()) // combine cost improvement between A and Aold
                Aold = A;
                N = generateNeighbours(Aold) // generate all neighbors by copy!
                A = localChoice(N,A,p)  // select best neighbour with probability p, else select A
            return A
         */

        // initialization
        A = new CentralPlan(allVehicles,taskSet);
        Aold = new CentralPlan(A);
        if (!A.isFeasible){
            System.out.println("WARNING: your problem is not feasible");
        }
        int iter=0;
        while(!goodEnough(A, Aold)) {

            Aold = new CentralPlan(A);
            System.out.println("will genrate neighbboor, iter: "+iter);
            List<CentralPlan> N = generateNeighbour(Aold, allVehicles);
            System.out.println("will do localChoice, iter: "+iter);
            A = localChoice(N, Aold, p);
            iter++;
        }
    }
    /*
    List<Plan> reconstructPlan(CentralPlan A, List<vehicle> vehicles){
        List<Plan> plans = new ArrayList<Plan>();

        for v in Vehicles
            city currentCity=vehicle.getCurrentCity()

            new plan= new plan(currentCity)

            for task in A.content.get(vehicle) // will pick the correct one

                // finding where the next city is
                nextCity=null
                if (pickup)
                    nextCity=task.PICKUPCITY
                elif(deliver)
                    nextCity=task.DELIVERCITY
                else
                    print(WARRRNING)

                // finding the path to this city
                List<City> path= currentCity.pathTo(tas);
                for nextCity in path:
                    plan.appendMove(nextCity))

                //appending the action
                if ExTask.actionType =="deliver"
                    plan.appendDelivery(Task)
                elif ExTask.actionType== "pickup"
                    plan.appendPickup(Task)
                else:
                   print WARING

            plans.append(plan)
    }
     */
    public List<Plan> reconstructPlan( List<Vehicle> allVehicles){

        List<Plan> plans = new ArrayList<Plan>();
        for (Vehicle v : allVehicles){
            City currentCity=v.getCurrentCity();
            Plan plan = new Plan(currentCity);

            for (ExTask t : this.A.content.get(v)){
                //finding where the next city is
                City nextCity=null;
                if (t.actionType== ExTask.ActionType.PICKUP){
                    nextCity=t.task.pickupCity;
                }else if(t.actionType== ExTask.ActionType.DELIVERY){
                    nextCity=t.task.deliveryCity;
                }else{
                    throw new IllegalNameException("problem with type of extask");
                }
                // finding the path to this city
                List<City> path= currentCity.pathTo(nextCity);
                for (City c :path){
                    plan.appendMove(c);
                }
                currentCity=nextCity;  // corrected this dumb mistake
                // appending the action
                if(t.actionType==ExTask.ActionType.PICKUP){
                    plan.appendPickup(t.task);
                }else if (t.actionType==ExTask.ActionType.DELIVERY){
                    plan.appendDelivery(t.task);
                }else{
                    throw new IllegalNameException("problem with appending the action");
                }

            }
            plans.add(plan);
        }
        return plans;
    }




    private boolean goodEnough(CentralPlan A, CentralPlan Aold){
        double changeRatio= Math.abs(CentralPlan.computeCost(A)-CentralPlan.computeCost(Aold))/CentralPlan.computeCost(Aold);
        double eps=1e-3;
        double maxIterWithNoChange=20;

        if (changeRatio<eps){
            iterWithNoChange++;
        }else{
            iterWithNoChange=0;
        }

        if (iterWithNoChange>maxIterWithNoChange){
            return true;
        }else{
            return false;
        }

    }

    private List<CentralPlan> generateNeighbour(CentralPlan Aold, List<Vehicle> allVehicles){
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
        List<CentralPlan> N = new ArrayList<CentralPlan>(); // neighbour plans are a list of HashMap

        Vehicle v1 = selectRandomVehicle(Aold,allVehicles);
        for(Vehicle v2 : allVehicles){
            if(CentralPlan.canChangeVehicle(Aold,v1,v2)){
                CentralPlan Anew = CentralPlan.changeVehicle(Aold, v1, v2);
                N.add(Anew);
            }
        }

        for (int idx1=0; idx1<Aold.content.get(v1).size()-1;idx1++){
            for(int idx2=idx1+1; idx2<Aold.content.get(v1).size();idx2++){
                if(Aold.canSwap(Aold,v1,idx1,idx2)){
                    CentralPlan Anew = Aold.swapTask(Aold,v1,idx1,idx2);
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



    public Vehicle selectRandomVehicle(CentralPlan Aold, List<Vehicle> allVehicles){
        // select a vehicle with a nonempty task set
        int nTasks = 0;
        Vehicle vehicle;
        do{
            int randIdx = (int) (Math.random()*allVehicles.size());
            vehicle = allVehicles.get(randIdx);
            nTasks = Aold.content.get(vehicle).size(); //THIS FUCKER IS WEIRD !!!!!!!!!!!!!!!
        }while(nTasks==0);

        return vehicle;
    }
    //localChoice(centraPlanSet, centraPlan)()  //will pick Best Neighboor



    //getOptimalPlan()

    //TODO implemnt pseudo code
    /*
    Void localchoice(List<Hmap> n, p, Aold){
        Hmap bestNeighboor=getBestNeighboor(n)

        if(randomComputing(p)){
            Return Aold
        }else{
            Return BestNeighboor
          }

    }
     */
    public CentralPlan localChoice(List<CentralPlan> neighbours, CentralPlan Aold, double p){
        CentralPlan bestNeighbour=CentralPlan.getBestNeighbour(neighbours);
        if(Math.random()<p){   //p=0.3 ou 0.5   //p give the new plan //    (1-P) give the old one
            //give the new plan
            return bestNeighbour;
        }else{
            //give the old plan
            return Aold;
        }
    }


}
