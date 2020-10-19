import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;


@SuppressWarnings("unused")
public class Deliberative implements DeliberativeBehavior {

    enum Algorithm { BFS, ASTAR }

    /* Environment */
    Topology topology;
    TaskDistribution td;

    /* the properties of the agent */
    Agent agent;
    int capacity;

    /* the planning class */
    Deliberative.Algorithm algorithm;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {
        this.topology = topology;
        this.td = td;
        this.agent = agent;

        // initialize the planner
        int capacity = agent.vehicles().get(0).capacity();
        String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

        // Throws IllegalArgumentException if algorithm is unknown
        algorithm = Deliberative.Algorithm.valueOf(algorithmName.toUpperCase());


    }

    @Override
    public Plan plan(Vehicle vehicle, TaskSet tasks) {
        Plan plan;
        long timeStart = System.currentTimeMillis();

        // Compute the plan with the selected algorithm.
        switch (algorithm) {
            case ASTAR:

                ASTAR AStar = new ASTAR(vehicle, tasks);
                plan = AStar.backtrackPath(vehicle);
                break;
            case BFS:

                BFS optimizer;
                optimizer = new BFS(vehicle, tasks);
                plan = optimizer.backtrackPath(vehicle);
                break;
            default:
                throw new AssertionError("Should not happen.");
        }
        long timeEnd = System.currentTimeMillis();
        System.out.printf("planningDuration(MS): %d%n", timeEnd - timeStart);
        return plan;
    }

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity))
                plan.appendMove(city);

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path())
                plan.appendMove(city);

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }

    @Override
    public void planCancelled(TaskSet carriedTasks) {

        if (!carriedTasks.isEmpty()) {
            // This cannot happen for this simple agent, but typically
            // you will need to consider the carriedTasks when the next
            // plan is computed.
        }
    }
}
