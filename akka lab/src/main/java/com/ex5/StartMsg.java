package com.ex5;

import akka.actor.ActorRef;

public class StartMsg {
    private ActorRef serverRef;

    public StartMsg(ActorRef serverRef) {
        this.serverRef = serverRef;
    }

    public void setServerRef(ActorRef serverRef) {
        this.serverRef = serverRef;
    }

    public ActorRef getServerRef() {
        return serverRef;
    }
}
