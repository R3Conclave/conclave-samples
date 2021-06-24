package net.corda.samples.trade;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import net.corda.samples.trade.common.ModelSerializer;
import net.corda.samples.trade.common.TradeModel;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class KryoTest {
    private Kryo kryo;
    private TradeModel tradeModel;

    @Before
    public void setup(){
        kryo = new Kryo();
        tradeModel = new TradeModel(UUID.randomUUID().toString(), "buyOrderRef", 100, "TCS", 60.6, "buyer", "seller");


    }

    @Test
    public void testSerialization(){
        Output output = serializeTrade(tradeModel);
        byte[] bytes  = output.getBuffer();

        TradeModel tradeModel = deSerializeTrade(bytes);
        System.out.println(tradeModel);

    }


    private Output serializeTrade(TradeModel trade){
        Output output = new Output(new ByteArrayOutputStream());
        kryo.register(TradeModel.class, new ModelSerializer.TradeSerializer());
        kryo.writeObject(output, trade);
        output.flush();
        output.close();
        return output;
    }

    private TradeModel deSerializeTrade(byte[] tradeBytes){
        Kryo kryo = new Kryo();
        kryo.register(TradeModel.class, new ModelSerializer.TradeSerializer());
        Input input = new Input(new ByteArrayInputStream(tradeBytes));
        TradeModel tradeModel = kryo.readObject(input, TradeModel.class);
        return tradeModel;
    }
}
