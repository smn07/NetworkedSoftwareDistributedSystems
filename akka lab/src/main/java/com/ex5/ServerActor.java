package com.ex5;

import akka.actor.AbstractActorWithStash;
import akka.actor.Props;

import java.util.HashMap;
import java.util.Map;

public class ServerActor extends AbstractActorWithStash {
	private int putMessagesReceived = 0;

	private Map<String, String> emailsAddr = new HashMap<>();

	public ServerActor() {

	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(PutMsg.class, this::addContact)
				.match(GetMsg.class, this::getContact)
				.build();
	}

	void addContact(PutMsg msg) throws Exception {
		if(putMessagesReceived >= 1){
			putMessagesReceived = 0;
			// simulate a fault
			System.out.println("I am emulating a FAULT!");
			throw new Exception("Actor fault!");
		}else{
			emailsAddr.put(msg.getName(), msg.getEmailAddr());
			putMessagesReceived++;
		}
	}

	void getContact(GetMsg msg){
		String email = emailsAddr.get(msg.getName());

		ReplyMsg reply = new ReplyMsg(msg.getName(), email);

		getSender().tell(reply, this.getSelf());
	}

	static Props props() {
		return Props.create(ServerActor.class);
	}

}
