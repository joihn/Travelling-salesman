import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;

public class ASTAR(Vehicle vehicle_, TaskSet taskSet_) {
    Plan bestPlan;
    State finalNode;


    public ASTAR(Vehicle vehicle_, TaskSet taskSet_){
        State initialNode = new State(vehicle_.getCurrentCity(), vehicle_, vehicle_.getCurrentTasks(),taskSet_, null,null);

        ArrayList<State> C = new ArrayList<State>();
        Queue<State> Q = new PriorityQueue<State>();

        Q.add(initialNode);
        boolean finalNodeReached = false;

        while(!finalNodeReached){
            State n = Q.remove();
            if (!IsNinC(n,C) || (n.parent.cost + n.currentCity.distanceTo(n.parent.currentCity)) < getValueOfNInC(n, C)){
                C.add(n);

                ArrayList<State> children = new ArrayList<State>(n.getChildren(vehicle_));
                for (State childNode : children) {
                    if (!isChildInQ(childNode,Q)) {
                        Q.add(childNode);
                    } else {
                        // child is already in Q
                        // modify cost and costPlusH
                        checkAndReplaceNode(childNode,Q);
                    }
                }

            } else {
                System.out.println("Cycle avoided. Didn't add node to C again");
            }
            // TODO change finalNodeReached to true if n is a final node
            if (isFinalNode(n)){
                finalNodeReached = true;
            }

        }



    }

    boolean IsNinC(State n, ArrayList<State> C){
        // check if the node is already contained in C
        for(State stateC: C) {
            if ((stateC.currentCity.equals(n.currentCity)) && (stateC.tasksToDeliver.equals(n.tasksToDeliver)) && (stateC.tasksAvailable.equals(n.tasksAvailable))) {
                //System.out.println("IsNInC : true");
                return true;
            }
        }
        return false;
    }

    boolean isChildInQ(State child, Queue<State> Q ){
        for(State stateInQ : Q) {
            if ((stateInQ.currentCity.equals(child.currentCity)) && (stateInQ.tasksToDeliver.equals(child.tasksToDeliver)) && (stateInQ.tasksAvailable.equals(child.tasksAvailable))) {
                //System.out.println("IsNInC : true");
                return true;
            }
        }
        return false;
    }

    void checkAndReplaceNode(State child, Queue<State> Q){
        for(State stateInQ: Q){
            if ((stateInQ.currentCity.equals(child.currentCity)) && (stateInQ.tasksToDeliver.equals(child.tasksToDeliver)) && (stateInQ.tasksAvailable.equals(child.tasksAvailable))) {
                //System.out.println("IsNInC : true");
                if (stateInQ.costPlusH > child.costPlusH){
                    stateInQ.cost = child.cost;
                    stateInQ.costPlusH = child.costPlusH;
                    stateInQ.parent = child.parent;
                }
                return;
            }
        }
    }

    public double getValueOfNInC(State n, ArrayList<State> C){
        System.out.println("The size of C is: " + C.size());
        for(State stateC: C) {
            if (stateC.currentCity.equals(n.currentCity) && stateC.tasksToDeliver.equals(n.tasksToDeliver)  && stateC.tasksAvailable.equals(n.tasksAvailable)) {
                //System.out.println("getCostOfNInC returned the value : " + stateC.cost);
                return stateC.cost;
            }
        }
        System.out.println("WARNING ! DIDN'T FIND THIS STATE IN C");
        return 0.0;
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
