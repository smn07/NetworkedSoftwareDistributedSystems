
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import static akka.pattern.Patterns.ask;
import static java.util.concurrent.TimeUnit.SECONDS;

public class SensorDataProcessor {

	public static void main(String[] args) {

		// Number of sensors for testing
		final int NO_SENSORS = 4;
		
		// Number of sensor readings to generate
		final int SENSING_ROUNDS = 1;

		// timeout
		scala.concurrent.duration.Duration timeout = scala.concurrent.duration.Duration.create(5, SECONDS);
		
		final ActorSystem sys = ActorSystem.create("System");

		// Create sensor actors
		List<ActorRef> sensors = new LinkedList<ActorRef>();
		for (int i = 0; i < NO_SENSORS; i++) {
			sensors.add(sys.actorOf(TemperatureSensorActor.props(), "t" + i));
		}

		// Create dispatcher
		final ActorRef dispatcher = sys.actorOf(DispatcherActor.props(), "dispatcher");

		SetDispatcherMsg setDispatcherMsg = new SetDispatcherMsg(dispatcher);

		for (ActorRef sensor : sensors) {
			sensor.tell(setDispatcherMsg, ActorRef.noSender());
		}

		// Create processors
		scala.concurrent.Future<Object> waitingForProcessor;

		try {
			for(int i = 0; i < DispatcherActor.getNoProcessors(); i++){
				// create a processor using the dispatcher
				waitingForProcessor = ask(dispatcher, Props.create(SensorProcessorActor.class), 5000);
				ActorRef processor = (ActorRef) waitingForProcessor.result(timeout, null);

				// send the reference of the processor to the dispatcher
				SetProcessorMsg processorMsg = new SetProcessorMsg(processor);
				dispatcher.tell(processorMsg, ActorRef.noSender());
			}
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		// Waiting until system is ready
		try {
			SECONDS.sleep(1);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Generate some temperature data
		for (int i = 0; i < SENSING_ROUNDS; i++) {
			for (ActorRef t : sensors) {
				t.tell(new GenerateMsg(), ActorRef.noSender());
			}
		}

		// Waiting for temperature messages to arrive
		try {
			SECONDS.sleep(1);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Re-configure dispatcher to use Round Robin
		DispatchLogicMsg dispatchLogicMsg = new DispatchLogicMsg(0);
		dispatcher.tell(dispatchLogicMsg, ActorRef.noSender());
		
		// Waiting for dispatcher reconfiguration
		try {
			SECONDS.sleep(1);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Generate some more temperature data
		for (int i = 0; i < SENSING_ROUNDS+1; i++) {
			for (ActorRef t : sensors) {
				t.tell(new GenerateMsg(), ActorRef.noSender());
			}
		}
		
		// A new (faulty) sensor joins the system
		ActorRef faultySensor = sys.actorOf(TemperatureSensorFaultyActor.props(), "tFaulty");
		sensors.add(0, faultySensor);

		// send the dispatcher reference to the faulty sensor
		faultySensor.tell(new SetDispatcherMsg(dispatcher), ActorRef.noSender());

		// Wait until system is ready again
		try {
			SECONDS.sleep(1);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Generate some more temperature data
		for (int i = 0; i < SENSING_ROUNDS+1; i++) {
			for (ActorRef t : sensors) {
				t.tell(new GenerateMsg(), ActorRef.noSender());
			}
		}
	}
}
