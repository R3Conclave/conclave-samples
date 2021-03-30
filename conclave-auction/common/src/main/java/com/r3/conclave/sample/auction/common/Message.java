package com.r3.conclave.sample.auction.common;


import java.io.Serializable;

public class Message implements Serializable {

    private String type;
    private Integer bid;

    public Message(String type, Integer bid) {
        this.type = type;
        this.bid = bid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getBid() {
        return bid;
    }

    public Message(Integer bid) {
        this.bid = bid;
    }
}
