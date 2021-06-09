import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.r3.conclave.sample.common.AdDetails;
import com.r3.conclave.sample.common.InputData;
import com.r3.conclave.sample.common.InputDataSerializer;
import com.r3.conclave.sample.common.UserDetails;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class test {



    @Test
    public void testt() {

        List<UserDetails> userDetailsList = new ArrayList();
        UserDetails userDetails1 = new UserDetails("123");
        UserDetails userDetails2 = new UserDetails("456");
        userDetailsList.add(userDetails1);
        userDetailsList.add(userDetails2);

        List<AdDetails> adDetailsList = new ArrayList();
        AdDetails adDetails1 = new AdDetails("123");
        AdDetails adDetails2 = new AdDetails("456");
        adDetailsList.add(adDetails1);
        adDetailsList.add(adDetails2);


        InputData inputData = new InputData(userDetailsList, adDetailsList);

        Output output = serializeMessage(inputData);
        InputData i = deserialize(output);
        System.out.println(i.getUserDetailsList().size());
        System.out.println(i.getAdDetailsList().size());


    }

    private static Output serializeMessage(InputData listOfCreditCardNumbers){
        Kryo kryo = new Kryo();
        Output output = new Output(new ByteArrayOutputStream());
        kryo.register(InputData.class, new InputDataSerializer());
        kryo.writeObject(output, listOfCreditCardNumbers);
        output.close();
        return output;
    }

    private InputData deserialize(Output output) {
        Kryo kryo = new Kryo();
        kryo.register(InputData.class, new InputDataSerializer());
        Input input = new Input(new ByteArrayInputStream(output.getBuffer()));
        return kryo.readObject(input, InputData.class);
    }
}
