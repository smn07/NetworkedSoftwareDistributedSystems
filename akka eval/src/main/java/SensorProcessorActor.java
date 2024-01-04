import akka.actor.AbstractActor;
import akka.actor.Props;

public class SensorProcessorActor extends AbstractActor {

	private double currentAverage = 0;

	private int numberOfMessages = 0;

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(TemperatureMsg.class, this::gotData).build();
	}

	private void gotData(TemperatureMsg msg) throws Exception {

		System.out.println("SENSOR PROCESSOR " + self() + ": Got data from " + msg.getSender());

		if(msg.getTemperature() < 0){
			System.out.println("SENSOR PROCESSOR " + self() + ": got a negative temperature value");
			throw new Exception("Negative Temperature");
		}else{
			System.out.println("SENSOR PROCESSOR " + self() + ": Old avg is " + currentAverage);

			currentAverage = (currentAverage*numberOfMessages + msg.getTemperature())/(numberOfMessages+1);

			numberOfMessages += 1;

			System.out.println("SENSOR PROCESSOR " + self() + ": Current avg is " + currentAverage);
		}
	}

	static Props props() {
		return Props.create(SensorProcessorActor.class);
	}

	public SensorProcessorActor() {
	}
}
