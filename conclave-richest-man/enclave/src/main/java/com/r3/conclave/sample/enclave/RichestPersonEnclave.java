package com.r3.conclave.sample.enclave;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.enclave.EnclavePostOffice;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.common.Message;
import com.r3.conclave.sample.common.MessageSerializer;
import com.r3.conclave.shaded.kotlin.Pair;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class RichestPersonEnclave extends Enclave {

    private Map<PublicKey, String> userRouteMap = new HashMap<>();
    private Map<PublicKey, Integer> userNetworthMap = new HashMap<>();
    private Kryo kryo = new Kryo();

    private Pair<String, PublicKey> adminClient = null;

    // Mails send from clients to the enclave are received here
    @Override
    protected void receiveMail(long id, EnclaveMail mail, String userRoute) {
        Message message = readMail(mail);
        PublicKey senderPK = mail.getAuthenticatedSender();
        if (message.getType().equals("NETWORTH")) {
            userNetworthMap.put(senderPK, message.getNetworth());
            userRouteMap.put(senderPK, userRoute);
        } else if (message.getType().equals("FIND-RICHEST")) {
            adminClient = new Pair<>(userRoute, senderPK);
            findRichest();
        }
    }

    private void findRichest() {
        PublicKey richest = null;
        int maxNetworth = 0;
        for (PublicKey publicKey : userNetworthMap.keySet()) {
            Integer networth = userNetworthMap.get(publicKey);

            if (networth > maxNetworth) {
                maxNetworth = networth;
                richest = publicKey;
            }
        }

        sendResult(richest);
    }

    // Send result back to the client
    private void sendResult(PublicKey richest){

        for(PublicKey publicKey: userRouteMap.keySet()){
            if(publicKey.equals(richest)){
                sendMail(publicKey, userRouteMap.get(publicKey), "You are the richest person");
            }else{
                sendMail(publicKey, userRouteMap.get(publicKey), "Client having " + richest.toString() +"as public key is the richest person");
            }
        }

        sendMail(adminClient.getSecond(), adminClient.getFirst(), "Client having " + richest.toString() +"as public key is the richest person" +
                "having networth of: " + userNetworthMap.get(richest));
    }

    private void sendMail(PublicKey key, String routingHint, String message) {
        EnclavePostOffice postOffice = this.postOffice(key);
        byte[] result = postOffice.encryptMail(message.getBytes());
        postMail(result, routingHint);
    }


    private Message readMail(EnclaveMail mail) {
        kryo.register(Message.class, new MessageSerializer());
        Input input = new Input(new ByteArrayInputStream(mail.getBodyAsBytes()));
        return kryo.readObject(input, Message.class);
    }

}