package com.r3.conclave.sample.common;

import java.io.Serializable;
import java.util.List;

/**
 * This is the custom object containing input data which is sent by client to enclave and vice versa
 */
public class InputData implements Serializable {

    List<UserDetails> userDetailsList;
    List<AdDetails> adDetailsList;

    public InputData() {
    }

    public InputData(List<UserDetails> userDetailsList, List<AdDetails> adDetailsList) {
        this.userDetailsList = userDetailsList;
        this.adDetailsList = adDetailsList;
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
