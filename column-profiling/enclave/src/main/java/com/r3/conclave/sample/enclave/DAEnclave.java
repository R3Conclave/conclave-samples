package com.r3.conclave.sample.enclave;

import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.enclave.EnclavePostOffice;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.dataanalysis.common.UserProfile;

import java.util.*;
import java.io.*;

// Return the frequency distribution of values passed by the client
public class DAEnclave extends Enclave {
    byte[] previousResult;

    public static String dataAnalysis(ArrayList<UserProfile> arr) {
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
            ArrayList list = new ArrayList<UserProfile>();
            ByteArrayInputStream bis = null;
            ObjectInputStream is = null;
            byte[] bytes = mail.getBodyAsBytes();
            try {
                if (bytes != null) {
                    bis = new ByteArrayInputStream(bytes);
                    is = new ObjectInputStream(bis);
                    while (true) {
                        Object obj = is.readObject();
                        if (obj == null) {
                            break;
                        } else {
                            list.add((UserProfile) obj);
                        }
                    }
                    is.close();
                    bis.close();
                }
                String result = dataAnalysis(list);
                // Get the post office object for responding back to this mail and use it to encrypt our response.
                final byte[] responseBytes = postOffice(mail).encryptMail(result.getBytes());
                postMail(responseBytes, routingHint);
            } catch (Exception e) {
                System.err.println("Failed to process mail received at enclave");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Failed to process mail received at enclave");
            e.printStackTrace();
        }

    }

}
