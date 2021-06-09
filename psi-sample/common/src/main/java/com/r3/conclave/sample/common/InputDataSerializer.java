package com.r3.conclave.sample.common;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;
import java.util.List;

/**
 * This is used by Kryo to serialize and deserialize custom objects when they are sent from client to enclave and vice versa
 */
public class InputDataSerializer extends Serializer<InputData> {

    @Override
    public void write(Kryo kryo, Output output, InputData object) {
        List<UserDetails> userDetailsList  = object.getUserDetailsList();
        List<AdDetails> adDetailsList  = object.getAdDetailsList();

        if(userDetailsList != null) {
            output.writeInt(userDetailsList.size());
            for (int i=0; i < userDetailsList.size(); i++) {
                output.writeString(userDetailsList.get(i).getCreditCardNumber());
            }
        } else {
            output.writeInt(0);
        }

        if(adDetailsList != null) {
            output.writeInt(adDetailsList.size());
            for (int i=0; i < adDetailsList.size(); i++) {
                output.writeString(adDetailsList.get(i).getCreditCardNumber());
            }
        } else {
            output.writeInt(0);
        }

    }

    @Override
    public InputData read(Kryo kryo, Input input, Class<InputData> type) {
        Integer userListSize = input.readInt();

        List<UserDetails> userDetailsList = new ArrayList<>(userListSize);
        for (int i=0; i< userListSize ; i++) {
            UserDetails userDetails = new UserDetails(input.readString());
            userDetailsList.add(userDetails);
        }

        Integer adListSize = input.readInt();
        List<AdDetails> adDetailsList = new ArrayList<>(adListSize);
        for (int i=0; i< adListSize ; i++) {
            AdDetails userDetails = new AdDetails(input.readString());
            adDetailsList.add(userDetails);
        }

        return new InputData(userDetailsList, adDetailsList);
    }

}
