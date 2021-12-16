package com.r3.conclave.sample.auction.client.springboot;

public class BidRequest {
    private String roleType;
    private int bid;

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }
}
