package com.ex3;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;

import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ClientActor extends AbstractActorWithStash {
	ActorRef serverRef;
	private int numMessage = 10;

	public ClientActor() {
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(StartMsg.class, this::start).build();
	}

	void start(StartMsg msg) throws InterruptedException {
		serverRef = msg.getServerRef();

		//put phase
		serverRef.tell(new PutMsg("name1", "email1"), this.getSelf());
		serverRef.tell(new PutMsg("name2", "email2"), this.getSelf());
		serverRef.tell(new PutMsg("name3", "email3"), this.getSelf());


		//get phase
		scala.concurrent.duration.Duration timeout = scala.concurrent.duration.Duration.create(5, SECONDS);
		scala.concurrent.Future<Object> waitForReply = Patterns.ask(serverRef, new GetMsg("name1"), 5000);

		try {
			ReplyMsg reply = (ReplyMsg) waitForReply.result(timeout, null);
			printAddress(reply);
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
		}

		waitForReply = Patterns.ask(serverRef, new GetMsg("name2"), 5000);

		try {
			ReplyMsg reply = (ReplyMsg) waitForReply.result(timeout, null);
			printAddress(reply);
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
		}
	}

	void printAddress(ReplyMsg msg){
		System.out.println("name: " + msg.getName() +
							"email: " + msg.getEmailAddr());
	}

	static Props props() {
		return Props.create(ClientActor.class);
	}

}
