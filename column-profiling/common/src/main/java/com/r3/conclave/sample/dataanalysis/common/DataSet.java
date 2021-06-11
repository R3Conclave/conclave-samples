package com.r3.conclave.sample.dataanalysis.common;

import java.io.Serializable;
import java.util.ArrayList;


public class DataSet implements Serializable {
    ArrayList<UserProfile> data;

    //creating a parameterized constructor
    public DataSet(ArrayList<UserProfile> u) {

        this.data = u;
    }

    public ArrayList<UserProfile> getList() {
        return data;
    }

}