package AuctionSmart;

import logist.task.Task;

public class ExTask {

    public enum ActionType{
        PICKUP,
        DELIVERY
    }
    public Task task;
    public ActionType actionType;

    public ExTask(Task task_, ActionType actionType_ ){
        this.task=task_;
        this.actionType=actionType_;
    }



}
