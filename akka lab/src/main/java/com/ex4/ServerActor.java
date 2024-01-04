package com.ex4;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ServerActor extends AbstractActorWithStash {

	public ServerActor() {
	}

	@Override
	public Receive createReceive() {
		return awake();
	}

	public Receive awake() {
		return receiveBuilder().match(TextMsg.class, this::replyTextMsg)
				.match(WaitMsg.class, this::setWait)
				.build();
	}

	public Receive asleep() {
		return receiveBuilder().match(TextMsg.class, this::stashTextMsg)
				.match(AwakeMsg.class, this::setAwake)
				.build();
	}

	void replyTextMsg(TextMsg msg){
		System.out.println("server: sending reply");
		ActorRef sender = msg.getSender();
		sender.tell(msg, this.getSelf());
	}

	void stashTextMsg(TextMsg msg){
		System.out.println("server: stashing message");
		stash();
	}

	void setWait(WaitMsg msg){
		System.out.println("server: going to sleep");
		getContext().become(asleep());
	}

	void setAwake(AwakeMsg msg){
		System.out.println("server: getting awake");
		getContext().become(awake());
		unstashAll();
	}

	static Props props() {
		return Props.create(ServerActor.class);
	}

}
