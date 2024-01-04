package com.ex4;

import akka.actor.ActorRef;

public class TextMsg {
    String text;
    ActorRef sender;

    public TextMsg(String text, ActorRef sender) {
        this.text = text;
        this.sender = sender;
    }

    public ActorRef getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }
}
