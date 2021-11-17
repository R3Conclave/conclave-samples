package com.r3.conclave.sample.common;

import java.io.Serializable;
import java.util.List;

/**
 * This is the custom object containing input data which is sent by client to enclave and vice versa
 */
public class InputData implements Serializable {


    private Role clientType;
    private List<UserDetails> userDetailsList;
    private List<AdDetails> adDetailsList;

    public InputData() {
    }

    public InputData(Role clientType, List<UserDetails> userDetailsList, List<AdDetails> adDetailsList) {
        this.clientType = clientType;
        this.userDetailsList = userDetailsList;
        this.adDetailsList = adDetailsList;
    }

    public Role getClientType() {
        return clientType;
    }

    public void setClientType(Role clientType) {
        this.clientType = clientType;
    }

    public List<UserDetails> getUserDetailsList() {
        return userDetailsList;
    }

    public void setUserDetailsList(List<UserDetails> userDetailsList) {
        this.userDetailsList = userDetailsList;
    }

    public List<AdDetails> getAdDetailsList() {
        return adDetailsList;
    }

    public void setAdDetailsList(List<AdDetails> adDetailsList) {
        this.adDetailsList = adDetailsList;
    }
}
