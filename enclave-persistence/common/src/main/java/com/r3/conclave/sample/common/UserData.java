package com.r3.conclave.sample.common;

import java.io.Serializable;

public class UserData implements Serializable {
    private final String username;
    private final String password;

    public UserData(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
