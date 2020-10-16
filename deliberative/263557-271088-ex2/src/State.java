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
import java.util.Collection;
import java.util.Comparator;

public class State  {
    public City currentCity;
    public TaskSet tasksToDeliver;
    public TaskSet tasksAvailable;
    public Action actionParent;
    public State parent;
    double cost; // cost from initial node
    double costPlusH; // cost from initial node + heuristic cost to final node

    public State(City currentCity, Vehicle vehicle, TaskSet tasksToDeliver_,TaskSet tasksAvailable_, Action actionParent, State parent){
        this.currentCity = currentCity;
        this.tasksToDeliver = tasksToDeliver_;
        this.tasksAvailable = tasksAvailable_;
        this.parent = parent;

        if (this.parent != null){ // initial node has no parent
            this.cost = this.parent.cost + this.currentCity.distanceTo(this.parent.currentCity);
            this.actionParent = actionParent;
        } else {
            this.cost = 0;
            this.actionParent = null;
        }
        this.costPlusH = this.cost + heuristic(currentCity,tasksToDeliver_,tasksAvailable_);
    }

    public ArrayList<State> getChildren(Vehicle vehicle){
        ArrayList<State> children = new ArrayList<State>();

        int currentWeight = getCurrentWeight();
        if (tasksAvailable!=null) {//TODO check if it actually fix this weird bug
            for (Task availableTask : tasksAvailable) {
                if (vehicle.capacity() >= currentWeight + availableTask.weight) {
                    // it is possible to pickup the task
                    City childCity = availableTask.pickupCity;
                    TaskSet nextTasksAvailable = tasksAvailable.clone();
                    nextTasksAvailable.remove(availableTask);

                    TaskSet nextTasksToDeliver = tasksToDeliver.clone();
                    nextTasksToDeliver.add(availableTask);

                    Action action = new Action.Pickup(availableTask);

                    State child = new State(availableTask.pickupCity, vehicle, nextTasksToDeliver, nextTasksAvailable, action, this);
                    children.add(child);
                }
            }
        }else{
            System.out.println("WARNING : taskavailable is null :( ");
        }
        if(tasksToDeliver!=null) { //TODO check if it actually fix this weird bug
            for (Task taskToDeliver : tasksToDeliver) {
                City childCity = taskToDeliver.deliveryCity;
                TaskSet nextTasksToDeliver = tasksToDeliver.clone();
                nextTasksToDeliver.remove(taskToDeliver);
                Action action = new Action.Delivery(taskToDeliver);
                State child = new State(taskToDeliver.deliveryCity, vehicle, nextTasksToDeliver, tasksAvailable, action, this);
                children.add(child);
            }
        }
        return children;
    }

    private int getCurrentWeight(){
        int weight = 0;
        for(Task task : tasksToDeliver){
            weight += task.weight;
        }
        return weight;
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



}
