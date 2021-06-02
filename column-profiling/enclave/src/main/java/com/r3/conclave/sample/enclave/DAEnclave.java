package com.r3.conclave.sample.enclave;

import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.enclave.EnclavePostOffice;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.dataanalysis.common.DataSet;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.r3.conclave.sample.dataanalysis.common.DataSetSerializer;
import com.r3.conclave.sample.dataanalysis.common.UserProfile;
import java.io.ByteArrayInputStream;
import java.util.*;


// Return the frequency distribution of values in Dataset
public class DAEnclave extends Enclave {
    private Kryo kryo = new Kryo();


    private static String dataAnalysis(ArrayList<UserProfile> arr) {
        Map<Integer, Integer> ageFreqDist = new HashMap<>();
        Map<String, Integer> countryFrqDist = new HashMap<>();
        Map<String, Integer> genderFreqDist = new HashMap<>();

        for( UserProfile user: arr){
            ageFreqDist.put(user.getAge(), ageFreqDist.get(user.getAge()) == null ? 1: ageFreqDist.get(user.getAge())+1);
            countryFrqDist.put(user.getCountry(), countryFrqDist.get(user.getCountry()) == null ? 1: countryFrqDist.get(user.getCountry())+1);
            genderFreqDist.put(user.getGender(), genderFreqDist.get(user.getAge()) == null ? 1: genderFreqDist.get(user.getAge())+1);
        }

        StringBuilder str
                = new StringBuilder();
        Iterator ageFreqIterator = ageFreqDist.entrySet().iterator();
        str.append("Age Frequency Distribution: ");
        while (ageFreqIterator.hasNext()) {
            Map.Entry pair = (Map.Entry)ageFreqIterator.next();
            str.append("{" + pair.getKey()+ ":" +pair.getValue()+"}");
            ageFreqIterator.remove(); // avoids a ConcurrentModificationException
        }

        Iterator countryFreqIterator = countryFrqDist.entrySet().iterator();
        str.append(" Country Frequency Distribution: ");
        while (countryFreqIterator.hasNext()) {
            Map.Entry pair = (Map.Entry)countryFreqIterator.next();
            str.append("{" + pair.getKey()+ ":" +pair.getValue()+"}");
            countryFreqIterator.remove(); // avoids a ConcurrentModificationException
        }

        Iterator genderFreqIterator = genderFreqDist.entrySet().iterator();
        str.append(" Gender Frequency Distribution: ");
        while (genderFreqIterator.hasNext()) {
            Map.Entry pair = (Map.Entry)genderFreqIterator.next();
            str.append("{" + pair.getKey()+ ":" +pair.getValue()+"}");
            genderFreqIterator.remove(); // avoids a ConcurrentModificationException
        }
        return str.toString();
    }

    @Override
    protected void receiveMail(long id, EnclaveMail mail, String routingHint) {
        // This is used when the host delivers a message from the client.
        // First, decode mail body
        DataSet ds = readMail(mail);

        try{
            Input input = new Input(mail.getBodyAsBytes());
            DataSet data = kryo.readObject(input, DataSet.class);
            ArrayList<UserProfile> u = data.getList();
            String result = dataAnalysis(u);
            // Get the post office object for responding back to this mail and use it to encrypt our response.
            final byte[] responseBytes = postOffice(mail).encryptMail(result.getBytes());
            postMail(responseBytes, routingHint);
        }
        catch (Exception e){
            System.out.println(e);
        };
    }


    private DataSet readMail(EnclaveMail mail) {
        Input input = new Input(new ByteArrayInputStream(mail.getBodyAsBytes()));
        kryo.register(DataSet.class, new DataSetSerializer());
        DataSet ds = kryo.readObject(input, DataSet.class);
        return ds;
    }

}
