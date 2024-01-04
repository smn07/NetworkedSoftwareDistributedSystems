package com.ex3;

import akka.actor.AbstractActorWithStash;
import akka.actor.Props;

import java.util.HashMap;
import java.util.Map;

public class ServerActor extends AbstractActorWithStash {

	private Map<String, String> emailsAddr = new HashMap<>();

	public ServerActor() {

	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(PutMsg.class, this::addContact)
				.match(GetMsg.class, this::getContact)
				.build();
	}

	void addContact(PutMsg msg){
		emailsAddr.put(msg.getName(), msg.getEmailAddr());
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
