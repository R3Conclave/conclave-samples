package com.r3.conclave.sample.common;

import org.tribuo.MutableDataset;
import java.io.Serializable;

/**
 * This is the custom object containing input data which is sent by client to enclave and vice versa
 */
public class InputData implements Serializable {
    private static final long serialVersionUID = 6529685098267757690L;

    private Role role;
    private MutableDataset trainingDataset;
    private MutableDataset testingDataset;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public InputData(Role role) {
        this.role = role;
    }
    public MutableDataset getTrainingDataset() {
        return trainingDataset;
    }

    public MutableDataset getTestingDataset() {
        return testingDataset;
    }

    public void setTrainingDataset(MutableDataset trainingDataset) {
        this.trainingDataset = trainingDataset;
    }

    public void setTestingDataset(MutableDataset testingDataset) {
        this.testingDataset = testingDataset;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
