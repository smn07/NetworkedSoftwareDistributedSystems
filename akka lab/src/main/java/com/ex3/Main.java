package com.ex3;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;

public class Main {

	private static final int numThreads = 10;
	private static final int numMessages = 100;

	public static void main(String[] args) {
		final ActorSystem sys = ActorSystem.create("System");
		final ActorRef server = sys.actorOf(ServerActor.props(), "server");
		final ActorRef client = sys.actorOf(ClientActor.props(), "client");

		client.tell(new StartMsg(server), ActorRef.noSender());
		
		// Wait for all messages to be sent and received
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sys.terminate();

	}

}
