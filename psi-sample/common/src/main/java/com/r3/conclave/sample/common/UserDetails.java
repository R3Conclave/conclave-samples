package com.r3.conclave.sample.common;

import java.io.Serializable;

public class UserDetails implements Serializable {

	private String creditCardNumber;
	private String userName;

	public UserDetails(String creditCardNumber) {
		this.creditCardNumber = creditCardNumber;
	}

	public String getCreditCardNumber() {
		return creditCardNumber;
	}

	public void setCreditCardNumber(String creditCardNumber) {
		this.creditCardNumber = creditCardNumber;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
