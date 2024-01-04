import akka.actor.ActorRef;

public class SetProcessorMsg {
    private ActorRef processor;

    public SetProcessorMsg(ActorRef processor) {
        this.processor = processor;
    }

    public ActorRef getProcessor() {
        return processor;
    }
}
