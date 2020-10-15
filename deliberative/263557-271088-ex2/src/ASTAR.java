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

    public ASTAR(Vehicle vehicle_, TaskSet taskSet_){
        this.initialCity = initialCity_;
        State initialNode= new State(vehicle_.getCurrentCity(),vehicle_,vehicle_.getCurrentTasks(), taskSet_, null, null);

        ArrayList<State> finalNodes = new ArrayList<State>();
        ArrayList<State> C = new ArrayList<State>();


    }
}
