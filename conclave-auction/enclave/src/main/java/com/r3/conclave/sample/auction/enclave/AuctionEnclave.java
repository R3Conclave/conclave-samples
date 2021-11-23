package com.r3.conclave.sample.auction.enclave;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.enclave.EnclavePostOffice;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.auction.common.Message;
import com.r3.conclave.sample.auction.common.MessageSerializer;
import com.r3.conclave.shaded.kotlin.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class AuctionEnclave extends Enclave {

    private Map<PublicKey, String> userRouteMap = new HashMap<>();
    private Map<PublicKey, Integer> userBidsMap = new HashMap<>();

    private Pair<String, PublicKey> auctionAdmin = null;

    // Mails send from clients to the enclave are received here
    @Override
    protected void receiveMail(EnclaveMail mail, String userRoute) {
        Message message = readMail(mail);
        PublicKey senderPK = mail.getAuthenticatedSender();
        if (message.getType().equals("BIDDER")) {
            userBidsMap.put(senderPK, message.getBid());
            userRouteMap.put(senderPK, userRoute);
        } else if (message.getType().equals("ADMIN")) {
            auctionAdmin = new Pair<>(userRoute, senderPK);
            processBids();
        }
    }

    // Process user bids. The highest bidder wins.
    private void processBids() {
        PublicKey winner = null;
        int maxBid = 0;
        for (PublicKey publicKey : userBidsMap.keySet()) {
            Integer bid = userBidsMap.get(publicKey);

            if (bid > maxBid) {
                maxBid = bid;
                winner = publicKey;
            }
        }

        sendAuctionResult(winner);
    }

    // Send auction result back to the client
    private void sendAuctionResult(PublicKey winner){

        for(PublicKey publicKey: userRouteMap.keySet()){
            if(publicKey.equals(winner)){
                sendMail(publicKey, userRouteMap.get(publicKey), "Congratulations! Your made the winning bid");
            }else{
                sendMail(publicKey, userRouteMap.get(publicKey), "Better Luck Next Time!");
            }
        }

        sendMail(auctionAdmin.getSecond(), auctionAdmin.getFirst(), "The winning bid is: " + userBidsMap.get(winner));

    }

    private void sendMail(PublicKey key, String routingHint, String message) {
        EnclavePostOffice postOffice = this.postOffice(key);
        byte[] result = postOffice.encryptMail(message.getBytes());
        postMail(result, routingHint);
    }


    private Message readMail(EnclaveMail mail) {
        ByteArrayInputStream in = new ByteArrayInputStream(mail.getBodyAsBytes());
        ObjectInputStream is;
        try {
            is = new ObjectInputStream(in);
            Message message = (Message) is.readObject();
            return message;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
