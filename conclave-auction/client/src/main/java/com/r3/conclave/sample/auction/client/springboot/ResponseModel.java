package com.r3.conclave.sample.auction.client.springboot;

public class ResponseModel {
    private boolean status;
    private String message;

    public ResponseModel(boolean status, String message){
        this.status = status;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        message = message;
    }
}
