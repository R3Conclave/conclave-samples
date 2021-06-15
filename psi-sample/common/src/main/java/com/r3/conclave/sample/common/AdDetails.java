package com.r3.conclave.sample.common;

import java.io.Serializable;

public class AdDetails implements Serializable {

    private String creditCardNumber;
    private String details;

    public AdDetails(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
