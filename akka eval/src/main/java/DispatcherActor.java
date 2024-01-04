import akka.actor.*;
import akka.japi.pf.DeciderBuilder;

import java.time.Duration;
import java.util.*;


public class DispatcherActor extends AbstractActorWithStash {

	private final static int NO_PROCESSORS = 2;

	private int currentRoundRobinProcessor = 0;

	private List<ActorRef> processors = new ArrayList<>();

	private Map<ActorRef, ActorRef> sensorProcessorMap = new HashMap<>();

	private static SupervisorStrategy strategy =
			new OneForOneStrategy(
					1,
					Duration.ofMinutes(1),
					DeciderBuilder.match(Exception.class, e -> SupervisorStrategy.resume())
			.build());


	public DispatcherActor() {
	}

	public static int getNoProcessors() {
		return NO_PROCESSORS;
	}

	@Override
	public SupervisorStrategy supervisorStrategy() {
		return strategy;
	}


	@Override
	public AbstractActor.Receive createReceive() {
		return loadBalance();
	}

	private void setLogicMessage(DispatchLogicMsg msg){
		if(msg.getLogic() == 1){
			// load balance
			System.out.println("DISPATCHER: setting dispatcher to load balance mode");
			getContext().become(loadBalance());
		}else{
			// round robin
			System.out.println("DISPATCHER: setting dispatcher to round robin mode");
			getContext().become(roundRobin());
		}
	}

	private Receive loadBalance(){
		return receiveBuilder()
				.match(
						Props.class,
						props -> {
							getSender().tell(getContext().actorOf(props), getSelf());
						})
				.match(DispatchLogicMsg.class, this::setLogicMessage)
				.match(SetProcessorMsg.class, this::setProcessor)
				.match(TemperatureMsg.class, this::dispatchDataLoadBalancer)
				.build();
	}

	private Receive roundRobin(){
		return receiveBuilder()
				.match(
						Props.class,
						props -> {
							getSender().tell(getContext().actorOf(props), getSelf());
						})
				.match(DispatchLogicMsg.class, this::setLogicMessage)
				.match(SetProcessorMsg.class, this::setProcessor)
				.match(TemperatureMsg.class, this::dispatchDataRoundRobin)
				.build();
	}

	private void setProcessor(SetProcessorMsg msg){
		processors.add(msg.getProcessor());
	}

	private void dispatchDataLoadBalancer(TemperatureMsg msg) {
		if(!sensorProcessorMap.containsKey(msg.getSender())){
			int processorIndex = sensorProcessorMap.size() % NO_PROCESSORS;
			sensorProcessorMap.put(msg.getSender(), processors.get(processorIndex));

			System.out.println("DISPATCHER: sensor processor map updated");
			System.out.println("DISPATCHER: sensor " + msg.getSender() + " processor " + processors.get(processorIndex));
		}else{
			ActorRef processor = sensorProcessorMap.get(msg.getSender());
			processor.tell(msg, self());
		}
	}

	private void dispatchDataRoundRobin(TemperatureMsg msg) {
		ActorRef processor = processors.get(currentRoundRobinProcessor);
		processor.tell(msg, self());

		currentRoundRobinProcessor = (currentRoundRobinProcessor + 1)%NO_PROCESSORS;
	}

	static Props props() {
		return Props.create(DispatcherActor.class);
	}
}
