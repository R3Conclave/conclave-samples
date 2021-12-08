package com.r3.conclave.sample.enclave;

import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.common.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is the Enclave class. To create an enclave in Conclave use a class that extends Enclave abstract class
 * and override the receiveMail method to receive the mail from the client.
 * This class contains the business logic to calculate the ad conversion rate, using the list of credit card numbers
 * given by the merchant and the service provider. Since the lists are shared with the enclave and not with anyone else,
 * complete privacy of data is guaranteed.
 */
public class PSIEnclave extends Enclave {

    private List<UserDetails> userDetailsList;
    private List<AdDetails> adDetailsList;

    private EnumMap<Role, String> clientToRoutingHint = new EnumMap<>(Role.class);
    private EnumMap<Role, PublicKey> clientToPublicKey = new EnumMap<>(Role.class);

    /**
     * This method gets called when client wants to communicate to enclave, and sends a message wrapped in a mail to host.
     * Host in turn calls deliverMail method which in turn
     * calls this method. In this method, we will deserialize the mail message, perform the computation and send the
     * result back to the clients.
     *
     * @param mail        decrypted and authenticated mail body
     * @param routingHint used by enclave to tell host whom to send the reply back to
     */
    @Override
    protected void receiveMail(EnclaveMail mail, String routingHint) {
        //Deserialize the mail object using custom deserializers
        InputData inputData = (InputData) deserialize(mail.getBodyAsBytes());

        if (inputData != null) {
            //Retrieve the clientType from mail.
            Role clientType = inputData.getClientType();

            /*Use clientToRoutingHint to store client routing information, which can be used
            while sending reply back to client*/
            clientToRoutingHint.put(clientType, routingHint);

            /*Use clientToPublicKey which identifies the key of the client, which can be used by
            postoffice to encrypt data and send to client*/
            clientToPublicKey.put(clientType, mail.getAuthenticatedSender());

            //Depending on the client type populate one of the two lists
            if (clientType == Role.SERVICE_PROVIDER) {
                adDetailsList = new ArrayList<>(inputData.getAdDetailsList().size());
                adDetailsList.addAll(inputData.getAdDetailsList());
            } else if (clientType == Role.MERCHANT) {
                userDetailsList = new ArrayList<>(inputData.getUserDetailsList().size());
                userDetailsList.addAll(inputData.getUserDetailsList());
            }

            //Once both the lists are populated, perform the ad conversion rate calculation
            if (userDetailsList != null && adDetailsList != null && !userDetailsList.isEmpty() && !adDetailsList.isEmpty()) {
                Double adConversionRate = getAdConversionRate(userDetailsList, adDetailsList);

                //send the ad conversion rate to merchant. use merchant's public key to encrypt the message
                //such that only merchant will be able to decrypt it
                byte[] encryptedReply = postOffice(clientToPublicKey.get(Role.MERCHANT)).
                        encryptMail(adConversionRate.toString().getBytes());
                postMail(encryptedReply, clientToRoutingHint.get(Role.MERCHANT));

                /*Send the ad conversion rate to service provider. use service provider's public key to
                encrypt the message such that only merchant will be able to decrypt it*/
                encryptedReply = postOffice(clientToPublicKey.get(Role.SERVICE_PROVIDER)).
                        encryptMail(adConversionRate.toString().getBytes());
                postMail(encryptedReply, clientToRoutingHint.get(Role.SERVICE_PROVIDER));
            }
        }
    }

    /**
     * This calculates ad conversion rate.
     * ad conversion rate = users who have made purchases / total users who have clicked the ads
     */
    public Double getAdConversionRate(List<UserDetails> userDetailsList, List<AdDetails> adDetailsList) {

        List<String> merchantCreditCardNumbers = new ArrayList<>();

        for (UserDetails userDetails : userDetailsList) {
            merchantCreditCardNumbers.add(userDetails.getCreditCardNumber());
        }

        List<String> serviceProviderCreditCardNumbers = new ArrayList<>();
        for (AdDetails adDetails : adDetailsList) {
            serviceProviderCreditCardNumbers.add(adDetails.getCreditCardNumber());
        }

        Set<String> usersWhoPurchasedAfterClickingAd = merchantCreditCardNumbers.stream()
                .distinct()
                .filter(serviceProviderCreditCardNumbers::contains)
                .collect(Collectors.toSet());

        return (Double.valueOf(usersWhoPurchasedAfterClickingAd.size()) / Double.valueOf(adDetailsList.size())) * 100;
    }

    /**
     * Using Kryo to deserialize object when passed from client to enclave
     *
     * @param data client request wrapped in mail object in bytes
     * @return deserialized input object
     */
    public static Object deserialize(byte[] data) {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is;
        try {
            is = new ObjectInputStream(in);
            return is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}