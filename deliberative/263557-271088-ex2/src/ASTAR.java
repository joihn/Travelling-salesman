import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import logist.plan.Action;
import logist.task.TaskSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Collections;

public class ASTAR{
    Plan optimalPlan ;
    State optimalFinalNode;

    public ASTAR(Vehicle vehicle_, TaskSet taskSet_){

        State initialNode = new State(vehicle_.getCurrentCity(), vehicle_, vehicle_.getCurrentTasks(), taskSet_, null, null);

        ArrayList<State> finalNodes = new ArrayList<State>();
        ArrayList<State> C = new ArrayList<State>();

        Queue<State> Q = new LinkedList<State>();
        Q.add(initialNode);

        int iter = 0;
        while (!Q.isEmpty()) {
            if (iter % 100 == 0) {
                System.out.println("iter is : " + iter);
            }
            iter++;
            System.out.println("Size of Q: " + Q.size());
            State n = Q.remove();
            if (!IsNInC(n, C) || (n.parent.cost + n.currentCity.distanceTo(n.parent.currentCity)) < getCostOfNInC(n, C)) {
                //add n to C
                C.add(n);

                if (n.getChildren(vehicle_).isEmpty()) {
                    //it's a final node !
                    System.out.println("We found a final node !");
                    finalNodes.add(n);

                } else {

                    ArrayList<State> children = new ArrayList(n.getChildren(vehicle_));
                    for (State childNode : children) {
                        if (!isChildInQ(childNode,Q)) {
                            Q.add(childNode);
                        } else {
                            checkAndReplaceNode(childNode,Q);
                        }
                    }

                    //Q.addAll(n.getChildren(vehicle_));// warnirng it's not a collection
                }

            } else {
                System.out.println("CYCLE AVOIDED ! the node wasn't added to C, and it's neighborr not added to Q");
            }
        }
        System.out.println(" finished exploring all the tree :D  ");

        // comparing which final node is the best
        int indexOfOptimal = 0;
        double minCostSoFar = Double.POSITIVE_INFINITY;
        for (int i = 0; i < finalNodes.size(); i++) {
            State stateFinal = finalNodes.get(i);
            if (stateFinal.cost < minCostSoFar) {
                indexOfOptimal = i;
            }
            i++;
        }
        optimalFinalNode = finalNodes.get(indexOfOptimal);

        BacktrackPath(vehicle_);

    }



    public double heuristic(City currentCity, TaskSet tasksToDeliver, TaskSet tasksAvailable ) {
        double maxCost = 0;
        // take the max cost of 1 task, looking trough all tasksAvailable
        for (Task taskAvailable : tasksAvailable) {
            double cost = currentCity.distanceTo(taskAvailable.pickupCity) + taskAvailable.pickupCity.distanceTo(taskAvailable.deliveryCity);
            if (cost > maxCost) {
                maxCost = cost;
            }
        }
        // take the max cost of 1 task, looking trough all taskToDeliver
        for (Task taskToDeliver : tasksToDeliver) {
            double cost = currentCity.distanceTo(taskToDeliver.deliveryCity);
            if (cost > maxCost) {
                maxCost = cost;
            }
        }
        return maxCost;
    }

    public boolean isFinalNode(State node){
        return node.tasksToDeliver.isEmpty() && node.tasksAvailable.isEmpty() ;
    }




}
