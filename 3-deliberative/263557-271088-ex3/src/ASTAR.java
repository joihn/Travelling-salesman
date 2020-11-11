import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology.City;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.*;

public class ASTAR {
    Plan bestPlan;
    State finalNode;


    public ASTAR(Vehicle vehicle_, TaskSet taskSet_){

        Comparator<State> stateComparator = new Comparator<State>() {
            @Override
            public int compare(State state1, State state2) {
                // returns a value >0 if state1.cost > state2.cost
                // returns 0 if both are equal
                if (state1.costPlusH - state2.costPlusH > 0) {
                    return 1;
//                    return -1;
                } else if (state1.costPlusH - state2.costPlusH < 0) {
                    return -1;
//                    return 1;
                } else {
                    return 0;
                }
            }
        };


        State initialNode = new State(vehicle_.getCurrentCity(), vehicle_, vehicle_.getCurrentTasks(),taskSet_, null,null);

        ArrayList<State> C = new ArrayList<State>();
        Queue<State> Q = new PriorityQueue<State>(stateComparator);

        Q.add(initialNode);
        boolean finalNodeReached = false;
        int iter=0;
        while(!finalNodeReached){
            State n = Q.poll();
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
//                System.out.println("Cycle avoided. Didn't add node to C again");
            ;
            }

            if (isFinalNode(n)){
                finalNodeReached = true;
                finalNode = n;
            }
        iter++;
        }
        System.out.printf("ASTAR iter %d ", iter);



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
        int sameNodeDiscovered =0;
        for(State stateInQ: Q){
            if ((stateInQ.currentCity.equals(child.currentCity)) && (stateInQ.tasksToDeliver.equals(child.tasksToDeliver)) && (stateInQ.tasksAvailable.equals(child.tasksAvailable))) {
                //System.out.println("IsNInC : true");
                sameNodeDiscovered++;
                if (stateInQ.costPlusH > child.costPlusH){
                    stateInQ.cost = child.cost;
                    stateInQ.costPlusH = child.costPlusH;
                    stateInQ.parent = child.parent;
                    stateInQ.actionParent = child.actionParent;
                }
            }
        }
        if (sameNodeDiscovered!=1) {
            System.out.println("problem in checkAndReplaceNode() ! we discovered " + sameNodeDiscovered + " nodes instead of 1 Node !!!!");
        }
        return;
    }

    public double getValueOfNInC(State n, ArrayList<State> C){
        for(State stateC: C) {
            if (stateC.currentCity.equals(n.currentCity) && stateC.tasksToDeliver.equals(n.tasksToDeliver)  && stateC.tasksAvailable.equals(n.tasksAvailable)) {
                //System.out.println("getCostOfNInC returned the value : " + stateC.cost);
                return stateC.cost;
            }
        }
        System.out.println("WARNING ! DIDN'T FIND THIS STATE IN C");
        return 0.0;
    }

    public boolean isFinalNode(State node){
        return node.tasksToDeliver.isEmpty() && node.tasksAvailable.isEmpty() ;
    }

    public Plan backtrackPath(Vehicle vehicle){//backtracking the best route
        State currentNode= finalNode;
        List<State> stateTrajectory= new ArrayList<State>();
        List<City> path;
        while (currentNode!=null){
            stateTrajectory.add(currentNode);
            currentNode=currentNode.parent;
        }

        // inverse the list
        Collections.reverse(stateTrajectory);
        //optimalPlan= new Plan(vehicle_.getCurrentCity(), actions);
        bestPlan= new Plan(vehicle.getCurrentCity());
        for(State state : stateTrajectory){
            if (state.parent == null){
//                System.out.println("1st node - only get here once");
                ;
            } else {
                //System.out.println("starting another node ");
                path = state.parent.currentCity.pathTo(state.currentCity);
                if (path.size()>0){
                    for(City nextCity : path) {
                        bestPlan.appendMove(nextCity);
                    }
                }
                //System.out.println("finished another node, gonna add ");
                bestPlan.append(state.actionParent);
//                System.out.println("the plan is : " + state.actionParent.toString());
                //System.out.println("finished another node, ADDED ! ");
            }
        }


        System.out.printf("cost: %.0f ", finalNode.cost * vehicle.costPerKm());
        return bestPlan;
    }


}
