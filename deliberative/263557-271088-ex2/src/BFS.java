import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
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
public class BFS {
    Plan optimalPlan ;
    State optimalFinalNode;
    public BFS(Vehicle vehicle_, TaskSet taskSet_) {

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

    public void BacktrackPath(Vehicle vehicle){//backtracking the best route
        State currentNode= optimalFinalNode;
        List<State> stateTrajectory= new ArrayList<State>();
        List<City> path;
        while (currentNode!=null){
            stateTrajectory.add(currentNode);
            currentNode=currentNode.parent;
        }

        // inverse the list
        Collections.reverse(stateTrajectory);
        //optimalPlan= new Plan(vehicle_.getCurrentCity(), actions);
        optimalPlan= new Plan(vehicle.getCurrentCity());
        for(State state : stateTrajectory){
            if (state.parent == null){
                System.out.println("1st node - only get here once");
            } else {
                //System.out.println("starting another node ");
                path = state.parent.currentCity.pathTo(state.currentCity);
                if (path.size()>0){
                    for(City nextCity : path) {
                        optimalPlan.appendMove(nextCity);
                    }
                }
                //System.out.println("finished another node, gonna add ");
                optimalPlan.append(state.actionParent);
                System.out.println(state.actionParent.toString());
                //System.out.println("finished another node, ADDED ! ");
            }
        }
        System.out.println("Finished plan build");

    }


    boolean IsNInC(State n, ArrayList<State> C){ //TODO correct declaratio
        /*
        check: -current town
                - task avaialble
                - taskToDeliver
                and nothing else !

         */
        for(State stateC: C) {
            if ((stateC.currentCity.equals(n.currentCity)) && (stateC.tasksToDeliver.equals(n.tasksToDeliver)) && (stateC.tasksAvailable.equals(n.tasksAvailable))) {
                //System.out.println("IsNInC : true");
                return true;
            }
        }
        //System.out.println("IsNInC : false");
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
                if (stateInQ.cost > child.cost){
                    stateInQ.cost = child.cost;
                    stateInQ.parent = child.parent;
                }
                return;
            }
        }
    }



    double getCostOfNInC(State n, ArrayList<State> C){
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
}
