package com.r3.conclave.sample.dataanalysis.common;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;

public class DataSetSerializer extends Serializer<DataSet> {
    @Override
    public void write(Kryo kryo, Output output, DataSet object) {
        ArrayList<UserProfile> list = object.getList();
        int n = list.size();
        output.writeInt(n);
        for (int i = 0; i < n; i++) {
            UserProfile user = list.get(i);
            output.writeString(user.getName());
            output.writeInt(user.getAge());
            output.writeString(user.getCountry());
            output.writeString(user.getGender());
        }
    }

    @Override
    public DataSet read(Kryo kryo, Input input, Class<DataSet> type) {
        Integer n = input.readInt();
        ArrayList<UserProfile> list = new ArrayList<UserProfile>(n);
        for (int i = 0; i < n; i++) {
            String name = input.readString();
            Integer age = input.readInt();
            String country = input.readString();
            String gender = input.readString();
            UserProfile u = new UserProfile(name, age, country, gender);
            list.add(u);
        }
        DataSet d = new DataSet(list);
        return d;


    }
}
