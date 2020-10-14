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

public class State {
    public City currentCity;
    public TaskSet tasksToDeliver;
    public TaskSet tasksAvailable;
    public Action actionParent;
    public State parent;
    double cost;

    public State(Vehicle vehicle, TaskSet tasksAvailable, Action actionParent, State parent){
        this.tasksToDeliver = vehicle.getCurrentTasks();
        this.tasksAvailable = tasksAvailable;
        this.parent = parent;

        if (this.parent != null){ // initial node has no parent
            this.cost = this.parent.cost + this.currentCity.distanceTo(this.parent.currentCity);
            this.actionParent = actionParent;
            if(actionParent)
        } else {
            this.cost = 0;
            this.actionParent = null;
            this.currentCity = vehicle.getCurrentCity();
        }
    }

    public ArrayList<State> getChildren(Vehicle vehicle){
        ArrayList<State> children = new ArrayList<State>();

        int currentWeight = getCurrentWeight();
        for(Task availableTask : tasksAvailable){
            if(vehicle.capacity()>= currentWeight + availableTask.weight){
                // it is possible to pickup the task
                City childCity = availableTask.pickupCity;
                TaskSet nextTasksAvailable = tasksAvailable.clone();
                nextTasksAvailable.remove(availableTask);

                TaskSet nextTasksToDeliver = tasksToDeliver.clone();
                nextTasksToDeliver.add(availableTask);

                Action action = new Action.Pickup(availableTask);

                State child = new State(vehicle,nextTasksAvailable,action,this);
                children.add(child);
            }
        }
        for(Task taskToDeliver : tasksToDeliver){
            City childCity = taskToDeliver.deliveryCity;
            TaskSet nextTasksToDeliver = tasksToDeliver.clone();
            nextTasksToDeliver.remove(taskToDeliver);
            Action action = new Action.Delivery(taskToDeliver);
            State child = new State(vehicle,tasksAvailable,action,this);
            children.add(child);
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



}
