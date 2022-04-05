package com.r3.conclave.sample.common;

import java.io.Serializable;

/**
 * This is the custom object containing input data which is sent by client to enclave and vice versa
 */
public class InputData implements Serializable {

    private CommandType command;
    private UserData userData;


    public InputData(CommandType command, UserData userData) {
        this.command = command;
        this.userData = userData;
    }

    public CommandType getCommandType() {
        return this.command;

    }

    public UserData getUserData() {
        return userData;
    }
}
