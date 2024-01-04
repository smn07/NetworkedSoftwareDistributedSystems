package com.ex1;

public class UpdateMessage {
    private String operation;

    public UpdateMessage(String operation){
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
