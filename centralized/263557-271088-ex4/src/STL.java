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
    public double p ;  //TODO should we harcode this ??
    public double bestCostSoFar;
    public long iterationsToImprovement = 0;
    public int iterFarFromBest=0;

    public STL(TaskSet taskSet, List<Vehicle> allVehicles, long timeout_plan, double p_) {
        /* pseudo code:
            A = initializeA(taskset, vehicleset)

            while(!goodEnough()) // combine cost improvement between A and Aold
                Aold = A;
                N = generateNeighbours(Aold) // generate all neighbors by copy!
                A = localChoice(N,A,p)  // select best neighbour with probability p, else select A
            return A
         */

        long startTime=System.currentTimeMillis();
        this.p=p_;
        // initialization
        A = new CentralPlan(allVehicles,taskSet);
        Aold = new CentralPlan(A);
        if (!A.isFeasible){
            System.out.println("WARNING: your problem is not feasible");
        }
        int iter=0;
        while(stillImproving(A) && (System.currentTimeMillis()-startTime+1000)<timeout_plan) {

            Aold = new CentralPlan(A);
            //System.out.println("will generate neighboor, iter: "+iter);
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
        double cost = CentralPlan.computeCost(this.bestASoFar);
        System.out.println("Total cost is : "+ cost );
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
        double maxIterWithoutImprovmement = 1e7;
        if (this.bestASoFar==null) { //first iteration -> return true
            return true;
        }else {                     // all the other iter
            if (this.iterationsToImprovement>maxIterWithoutImprovmement){
                System.out.println("Converged due to stopping-criterion");
                return false;
            } else {
                return true;
            }
            /* Maxime Version
            double maxIterFarFromBest =10000;
            double closenessOfAToBest = CentralPlan.computeCost(this.bestASoFar)/CentralPlan.computeCost(A);
            //     = 0.99 : super close to best
            //     = 0.4    : far from best

            if (closenessOfAToBest<0.5){
                this.iterFarFromBest++;
            }else{
                this.iterFarFromBest=0;
            }

            if (this.iterFarFromBest>maxIterFarFromBest){
                System.out.println("Stopped due to divergence from best Solution");
                return false;
            }else{
                return true;
            }
            */
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

            int nTaskCarried= Aold.content.get(v1).size()/2;
            int nTaskTot=0;
            for(Vehicle vI:allVehicles){
                nTaskTot +=  Aold.content.get(vI).size()/2;
            }
            //change a vehicle
            if(CentralPlan.canChangeVehicle(Aold,v1,v2)) {
                CentralPlan Anew = CentralPlan.changeVehicle(Aold, v1, v2);
                nTaskCarried--;
                N.add(new CentralPlan(Anew)); // deep clone it on the fly to avoid distant modif

//                while(Math.random()<((double) nTaskCarried)/(((double) nTaskTot)*1.5)){
                while(Math.random()<0.0000000001){
                    if(CentralPlan.canChangeVehicle(Anew,v1,v2)) {
                        Anew = CentralPlan.changeVehicle(Anew, v1, v2);
                        nTaskCarried--;
                        N.add(new CentralPlan(Anew));
                    }
                }
            }




        }
        //swapTask // WE GENRATE TOOOO MANY !
//        for (int idx1=0; idx1<Aold.content.get(v1).size()-1;idx1++){
//            for(int idx2=idx1+1; idx2<Aold.content.get(v1).size();idx2++){


        if (Aold.content.get(v1).size()>2) {// only if vehicle has at least 3 task = 4 extask

            CentralPlan Anew = swapATask(v1, N, Aold);
            N.add(new CentralPlan(Anew));

            while (Math.random() < 0.00000001) { //todo change
                Anew = swapATask(v1, N, Anew);
                N.add(new CentralPlan(Anew));
            }

        }
        return N;
    }

    public CentralPlan swapATask(Vehicle v1, List<CentralPlan> N, CentralPlan Aold_){
        int i = 0;
        boolean swapped=false;
        CentralPlan Anew=null;
        while (!swapped){  // will try to swap 1 task only
            int idx1 = (int) (Math.random()*(Aold_.content.get(v1).size()-1));
            int idx2 = (int) (Math.random()*(Aold_.content.get(v1).size() - idx1)+idx1);
            if(Aold_.canSwap(Aold_,v1,idx1,idx2)){
                Anew = Aold_.swapTask(Aold_,v1,idx1,idx2);

                swapped=true;
            } else {
                i++;
                if (i>50){
                    System.out.println("Trapped in while loop");
                    Anew=Aold_;
                }
            }
        }
        return Anew;
    }

    public Vehicle selectRandomVehicle(CentralPlan Aold, List<Vehicle> allVehicles){
        // select a vehicle with a nonempty task set
        int nTasks = 0;
        Vehicle vehicle;
        do{
            int randIdx = (int) (Math.random()*allVehicles.size());
            vehicle = allVehicles.get(randIdx);
            nTasks = Aold.content.get(vehicle).size();
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
            this.bestCostSoFar = CentralPlan.computeCost(bestNeighbour);
//            System.out.println("Initial solution with cost " + this.bestCostSoFar);

        }
        if (CentralPlan.computeCost(bestNeighbour)<CentralPlan.computeCost(this.bestASoFar)){
            bestASoFar= bestNeighbour;
            this.bestCostSoFar = CentralPlan.computeCost(bestNeighbour);
//            System.out.println("Found better solution with cost " + this.bestCostSoFar);
//            System.out.println("Iterations to improvement " + this.iterationsToImprovement);
            this.iterationsToImprovement = 0;
        }
        this.iterationsToImprovement++;
        //continue searching
        if(Math.random()<p){   //p=0.3 ou 0.5   //p give the new plan //    (1-P) give the old one
            //give the new plan
            return bestNeighbour;
        }else{
            //give the old plan
            //return Aold;

            //give a random neighboor
            int index =(int) (Math.random()* neighbours.size());
            return neighbours.get(index);
        }

    }


}
