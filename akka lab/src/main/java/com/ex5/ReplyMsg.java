package com.ex5;

public class ReplyMsg {
    String name;
    String emailAddr;

    public ReplyMsg(String name, String emailAddr){
        this.name = name;
        this.emailAddr = emailAddr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmailAddr() {
        return emailAddr;
    }

    public void setEmailAddr(String emailAddr) {
        this.emailAddr = emailAddr;
    }
}
