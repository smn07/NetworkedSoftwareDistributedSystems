import akka.actor.ActorRef;

public class SetDispatcherMsg {
    private ActorRef dispatcher;

    public SetDispatcherMsg(ActorRef dispatcher) {
        this.dispatcher = dispatcher;
    }

    public ActorRef getDispatcher() {
        return dispatcher;
    }
}
