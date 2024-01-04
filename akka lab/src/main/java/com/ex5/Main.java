package com.ex5;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.faultTolerance.counter.CounterActor;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static akka.pattern.Patterns.ask;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Main {
	public static void main(String[] args) {
		scala.concurrent.duration.Duration timeout = scala.concurrent.duration.Duration.create(5, SECONDS);

		final ActorSystem sys = ActorSystem.create("System");
		final ActorRef supervisor = sys.actorOf(SupervisorActor.props(), "supervisor");

		ActorRef server;
		try {
			// Asks the supervisor to create the child actor and returns a reference
			scala.concurrent.Future<Object> waitingForServer = ask(supervisor, Props.create(ServerActor.class), 5000);
			server = (ActorRef) waitingForServer.result(timeout, null);

			final ActorRef client = sys.actorOf(ClientActor.props(), "client");

			client.tell(new StartMsg(server), ActorRef.noSender());

			sys.terminate();

		} catch (TimeoutException | InterruptedException e1) {

			e1.printStackTrace();
		}

		// Wait for all messages to be sent and received
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sys.terminate();

	}

}
