package com.r3.conclave.sample.common;


import java.io.Serializable;

public class Message implements Serializable {

    private String type;
    private Integer networth;

    public Message(String type, Integer networth) {
        this.type = type;
        this.networth = networth;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getNetworth() {
        return networth;
    }

    public void setNetworth(Integer networth) {
        this.networth = networth;
    }
}
