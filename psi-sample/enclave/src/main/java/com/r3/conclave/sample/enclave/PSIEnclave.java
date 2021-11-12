package com.r3.conclave.sample.enclave;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.common.AdDetails;
import com.r3.conclave.sample.common.InputData;
import com.r3.conclave.sample.common.InputDataSerializer;
import com.r3.conclave.sample.common.UserDetails;

import java.io.ByteArrayInputStream;
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

    private static final String MERCHANT = "MERCHANT";
    private static final String SERVICE_PROVIDER = "SERVICE-PROVIDER";
    private List<UserDetails> userDetailsList;
    private List<AdDetails> adDetailsList;

    private Map<String , String> clientToRoutingHint = new HashMap();
    private Map<String, PublicKey> clientToPublicKey = new HashMap();
    private Kryo kryo = new Kryo();


    /**
     * This method gets called when client wants to communicate to enclave, and sends a message wrapped in a mail to host.
     * Host in turn calls deliverMail method which in turn
     * calls this method. In this method, we will deserialize the mail message, perform the computation and send the
     * result back to the clients.
     * @param mail decrypted and authenticated mail body
     * @param routingHint used by enclave to tell host whom to send the reply back to
     */
    @Override
    protected void receiveMail(EnclaveMail mail, String routingHint) {
        //deserialize the mail object using custom deserializers
        InputData inputData = deserialize(mail);

        //retrieve the clientType from mail.
        String clientType = inputData.getClientType();

        //use clientToRoutingHint to store client routing information, which can be used
        //while sending reply back to client
        clientToRoutingHint.put(clientType, routingHint);

        //use clientToPublicKey which identifies the key of the client, which can be used by
        // postoffice to encrypt data and send to client
        clientToPublicKey.put(clientType, mail.getAuthenticatedSender());

        //depending on the client type populate one of the two lists
        if (SERVICE_PROVIDER.equals(clientType)) {
            adDetailsList = new ArrayList(inputData.getAdDetailsList().size());
            adDetailsList.addAll(inputData.getAdDetailsList());
        } else if (MERCHANT.equals(clientType)) {
            userDetailsList = new ArrayList(inputData.getUserDetailsList().size());
            userDetailsList.addAll(inputData.getUserDetailsList());
        }

        //once both the lists are populated, perform the ad conversion rate calculation
        if (userDetailsList != null && adDetailsList != null && !userDetailsList.isEmpty() && !adDetailsList.isEmpty()) {
            Double adConversionRate = getAdConversionRate(userDetailsList, adDetailsList);

            //send the ad conversion rate to merchant. use merchant's public key to encrypt the message
            //such that only merchant will be able to decrypt it
            byte[] encryptedReply = postOffice(clientToPublicKey.get(MERCHANT)).
                    encryptMail(adConversionRate.toString().getBytes());
            postMail(encryptedReply, clientToRoutingHint.get(MERCHANT));

            //send the ad conversion rate to service provider. use service provider's public key to
            //encrypt the message such that only merchant will be able to decrypt it
            encryptedReply = postOffice(clientToPublicKey.get(SERVICE_PROVIDER)).
                    encryptMail(adConversionRate.toString().getBytes());
            postMail(encryptedReply, clientToRoutingHint.get(SERVICE_PROVIDER));
        }
    }

    /**
     * This calculates ad conversion rate.
     * ad conversion rate = users who have made purchases / total users who have clicked the ads
     * @param userDetailsList
     * @param adDetailsList
     * @return ad conversion rate
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

        return  (new Double(usersWhoPurchasedAfterClickingAd.size()) / new Double(adDetailsList.size())) * 100;
    }

    /**
     * Using Kryo to deserialize object when passed from client to enclave
     * @param mail
     * @return deserialized input object
     */
    private InputData deserialize(EnclaveMail mail) {
        kryo.register(InputData.class, new InputDataSerializer());
        Input input = new Input(new ByteArrayInputStream(mail.getBodyAsBytes()));
        return kryo.readObject(input, InputData.class);
    }
}