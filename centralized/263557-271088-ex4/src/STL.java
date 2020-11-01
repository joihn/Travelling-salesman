import java.util.ArrayList;
import java.util.List;

import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.TaskSet;
import logist.topology.Topology.City;



public class STL {

    public CentralPlan A ;
    public CentralPlan Aold;
    public CentralPlan bestASoFar = null;
    public double p =0.4;  //TODO should we harcode this ??
    public int iterFarFromBest=0;

    public STL(TaskSet taskSet, List<Vehicle> allVehicles, long timeout_plan) {
        /* pseudo code:
            A = initializeA(taskset, vehicleset)

            while(!goodEnough()) // combine cost improvement between A and Aold
                Aold = A;
                N = generateNeighbours(Aold) // generate all neighbors by copy!
                A = localChoice(N,A,p)  // select best neighbour with probability p, else select A
            return A
         */
        long start_time=System.currentTimeMillis();

        // initialization
        A = new CentralPlan(allVehicles,taskSet);
        Aold = new CentralPlan(A);
        if (!A.isFeasible){
            System.out.println("WARNING: your problem is not feasible");
        }
        int iter=0;
        while(stillImproving(A) && (System.currentTimeMillis()-start_time+1000)<timeout_plan) {

            Aold = new CentralPlan(A);
            //System.out.println("will genrate neighbboor, iter: "+iter);
            List<CentralPlan> N = generateNeighbour(Aold, allVehicles);
            //System.out.println("will do localChoice, iter: "+iter);
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

            for (ExTask t : this.bestASoFar.content.get(v)){
                //finding where the next city is
                City nextCity=null;
                if (t.actionType== ExTask.ActionType.PICKUP){
                    nextCity=t.task.pickupCity;
                }else if(t.actionType== ExTask.ActionType.DELIVERY){
                    nextCity=t.task.deliveryCity;
                }else{
                    System.out.println("Problem with type of ExTask instance");
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
                    System.out.println("Problem appending an action");
                }

            }
            plans.add(plan);
        }
        return plans;
    }




    public boolean stillImproving(CentralPlan A){

        double maxIterFarFromBest =10000;

        if (this.bestASoFar==null) { //first iter
            return true;
        }else {                     // all the other iter
            double closenessOfAToBest = CentralPlan.computeCost(this.bestASoFar)/CentralPlan.computeCost(A);
            //     = 0.99 : super close to best
            //     = 0.4    : far from best
            if (closenessOfAToBest<0.5){
                this.iterFarFromBest++;
            }else{
                this.iterFarFromBest=0;
            }


            if (this.iterFarFromBest>maxIterFarFromBest){
                return false;
            }else{
                return true;
            }
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


        // CHANGEVEHICLE
        for(Vehicle v2 : allVehicles){
            if (v1==v2){
                continue; // avoid changing a task with itself
            }

            if(CentralPlan.canChangeVehicle(Aold,v1,v2)) {
                //System.out.println("------------------------------------------->    HEHE will change vehicle !!!");
                CentralPlan Anew = CentralPlan.changeVehicle(Aold, v1, v2);
                N.add(Anew);
            }

        }
        //swapTask // WE GENRATE TOOOO MANY !
//        for (int idx1=0; idx1<Aold.content.get(v1).size()-1;idx1++){
//            for(int idx2=idx1+1; idx2<Aold.content.get(v1).size();idx2++){

        boolean swapped=false;
        int iterSwap=0;
        while (swapped==false && iterSwap<20 ){  // will try to swap 1 task only // fail maximumm 20 times
            int idx1= (int) (Math.random()*(Aold.content.get(v1).size()-1));
            int idx2 = (int) (Math.random()*(Aold.content.get(v1).size() - idx1)+idx1);
    //        for(int idx2=idx1+1; idx2<Aold.content.get(v1).size();idx2++){
                if(Aold.canSwap(Aold,v1,idx1,idx2)){
                    CentralPlan Anew = Aold.swapTask(Aold,v1,idx1,idx2);
                    N.add(Anew);
                    swapped=true;
                }else{
                    iterSwap++;
                }
        }

        return N;
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

        //save best result so far
        if (this.bestASoFar==null){
            this.bestASoFar=bestNeighbour;
        }
        if (CentralPlan.computeCost(bestNeighbour)<CentralPlan.computeCost(this.bestASoFar)){
            bestASoFar= bestNeighbour;
        }

        //continue searching
        if(Math.random()<p){   //p=0.3 ou 0.5   //p give the new plan //    (1-P) give the old one
            //give the new plan
            return bestNeighbour;
        }else{
            //give the old plan
            return Aold;
        }


    }


}
