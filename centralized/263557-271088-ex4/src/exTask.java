import logist.task.Task;

public class exTask {

    public enum ActionType{
        PICKUP,
        DELIVERY
    }
    public Task task;
    public ActionType actionType;

    public exTask(Task task_, ActionType actionType_ ){
        this.task=task_;
        this.actionType=actionType_;
    }



}
