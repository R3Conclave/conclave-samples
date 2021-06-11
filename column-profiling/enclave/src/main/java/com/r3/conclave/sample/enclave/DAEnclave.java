package com.r3.conclave.sample.enclave;

import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.enclave.EnclavePostOffice;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.dataanalysis.common.DataSet;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.r3.conclave.sample.dataanalysis.common.DataSetSerializer;
import com.r3.conclave.sample.dataanalysis.common.UserProfile;

import java.util.*;

// Return the frequency distribution of values in Dataset
public class DAEnclave extends Enclave {
    private Kryo kryo;

    DAEnclave() {
        this.kryo = new Kryo();
        kryo.register(DataSet.class, new DataSetSerializer());

    }


    private static String dataAnalysis(ArrayList<UserProfile> arr) {
        Map<Integer, Integer> ageFreqDist = new HashMap<>();
        Map<String, Integer> countryFrqDist = new HashMap<>();
        Map<String, Integer> genderFreqDist = new HashMap<>();

        for (UserProfile user : arr) {
            ageFreqDist.merge(user.getAge(), 1, (oldValue, defaultValue) -> oldValue + 1);
            countryFrqDist.merge(user.getCountry(), 1, (oldValue, defaultValue) -> oldValue + 1);
            genderFreqDist.merge(user.getGender(), 1, (oldValue, defaultValue) -> oldValue + 1);
        }

        StringBuilder str
                = new StringBuilder();
        str.append("Age Frequency Distribution: ");
        str.append(ageFreqDist.toString());
        str.append(" Country Frequency Distribution: ");
        str.append(countryFrqDist.toString());
        str.append(" Gender Frequency Distribution: ");
        str.append(genderFreqDist.toString());
        return str.toString();
    }

    @Override
    protected void receiveMail(long id, EnclaveMail mail, String routingHint) {
        // This is used when the host delivers a message from the client.
        // First, decode mail body

        try {
            Input input = new Input(mail.getBodyAsBytes());
            DataSet data = kryo.readObject(input, DataSet.class);
            ArrayList<UserProfile> u = data.getList();
            String result = dataAnalysis(u);
            // Get the post office object for responding back to this mail and use it to encrypt our response.
            final byte[] responseBytes = postOffice(mail).encryptMail(result.getBytes());
            postMail(responseBytes, routingHint);
        } catch (Exception e) {
            System.err.println("Failed to process mail received at enclave");
            e.printStackTrace();
        }
        ;
    }
}
