package com.ex4;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ClientActor extends AbstractActorWithStash {
	ActorRef serverRef;
	scala.concurrent.duration.Duration timeout = scala.concurrent.duration.Duration.create(5, SECONDS);

	public ClientActor() {
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(StartMsg.class, this::start)
				.match(TextMsg.class, this::printText).build();
	}

	void start(StartMsg msg) {
		serverRef = msg.getServerRef();

		// send first message
		serverRef.tell(new TextMsg("text1", this.getSelf()), this.getSelf());

		// tell server to sleep
		System.out.println("client: sending wait message");
		serverRef.tell(new WaitMsg(), this.getSelf());

		// send first message
		serverRef.tell(new TextMsg("text2", this.getSelf()), this.getSelf());

		// tell server to awake
		System.out.println("client: sending awake mesage");
		serverRef.tell(new AwakeMsg(), this.getSelf());
	}

	void printText(TextMsg msg){
		System.out.println("client: text: " + msg.getText());
	}

	static Props props() {
		return Props.create(ClientActor.class);
	}

}
