package com.r3.conclave.sample.auction.client.springboot;

public class ConnectionRequest {
    private String hostUrl;
    private String constraint;

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    public String getConstraint() {
        return constraint;
    }

    public void setConstraint(String constraint) {
        this.constraint = constraint;
    }
}
