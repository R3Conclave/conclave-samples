package com.r3.conclave.sample.common;

import java.io.Serializable;

/**
 * This is the custom object containing input data which is sent by client to enclave and vice versa
 */
public class InputData implements Serializable {

    private CommandType commandType;
    private User user;

    public InputData() {
    }

    public InputData(CommandType commandType, User user) {
        this.commandType = commandType;
        this.user = user;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
