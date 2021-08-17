package com.r3.conclave.sample.common;

import org.tribuo.MutableDataset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the custom object containing input data which is sent by client to enclave and vice versa
 */
public class InputData implements Serializable {
    private static final long serialVersionUID = 6529685098267757690L;

    private String inputType;
    private MutableDataset trainingDataset;
    private MutableDataset testingDataset;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public InputData(String inputType) {
        this.inputType = inputType;
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

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }
}
