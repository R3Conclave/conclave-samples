package com.r3.conclave.sample.common;

import java.io.Serializable;

public class AdDetails implements Serializable {

    private String creditCardNumber;
    private String adDetails;

    public AdDetails(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public String getAdDetails() {
        return adDetails;
    }

    public void setAdDetails(String adDetails) {
        this.adDetails = adDetails;
    }
}
