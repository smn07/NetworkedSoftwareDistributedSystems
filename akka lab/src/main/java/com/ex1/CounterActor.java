package com.ex1;

import akka.actor.AbstractActor;
import akka.actor.Props;

public class CounterActor extends AbstractActor {

	private int counter;

	public CounterActor() {
		this.counter = 0;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(UpdateMessage.class, this::updateCounter).build();

		/*
		return receiveBuilder().match(IncrementMessage.class, this::incrementCounter)
				.match(DecrementMessage.class, this::decrementCounter)
				.build();

		 */
	}

	void updateCounter(UpdateMessage msg) {
		if(msg.getOperation().equals("increment")){
			++counter;
			System.out.println("Counter increased to " + counter);
		}else if(msg.getOperation().equals("decrement")){
			--counter;
			System.out.println("Counter decreased to " + counter);
		}else{
			System.out.println("Wrong message type");
		}

	}

	void incrementCounter(IncrementMessage msg) {
		++counter;
		System.out.println("Counter increased to " + counter);
	}

	void decrementCounter(DecrementMessage msg) {
		--counter;
		System.out.println("Counter decreased to " + counter);
	}

	static Props props() {
		return Props.create(CounterActor.class);
	}

}
