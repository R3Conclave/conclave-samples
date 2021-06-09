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
 * This class is the Enclave class. To create an enclave in Conclave extend your class by Enclave interface.
 * This class will contain the our business logic to calculate the ad conversion rate by using the list of credit card numbers given by the merchant
 * and the service provider. Since the lists are shared with the enclave and not with anyone else, complete privacy of data is guaranteed.
 */
public class PSIEnclave extends Enclave {

    private static final String MERCHANT = "MERCHANT";
    private static final String SERVICE_PROVIDER = "SERVICE-PROVIDER";
    private List<UserDetails> userDetailsList;
    private List<AdDetails> adDetailsList;

    private Map<String , String> routingHintMap = new HashMap();
    private Map<String, PublicKey> clientToPublicKey = new HashMap();
    private Kryo kryo = new Kryo();


    /**
     * This method gets called when client wants to communicate to enclave, and sends a message wrapped in a mail to host. Host in turn calls deliverMail method which in turn
     * calls this method. In this method, we will deserialize the mail message, perform the computation and send the result back to the clients.
     * @param id
     * @param mail
     * @param routingHint
     */
    @Override
    protected void receiveMail(long id, EnclaveMail mail, String routingHint) {

        //deserialize the mail object using custom deserializers
        InputData inputData = deserialize(mail);

        //retrieve the envelope from mail. envelope is not encrypted
        String envelope = new String(mail.getEnvelope());

        //use routingHintMap to store client routing information, whcih can be used while sending reply back to client
        routingHintMap.put(envelope, routingHint);

        //use clientToPublicKey which identifies the key of the client, which can be used by postoffice to encrypt data and send to client
        clientToPublicKey.put(envelope, mail.getAuthenticatedSender());

        //depending on the client type populate the two lists
        if (SERVICE_PROVIDER.equals(envelope)) {
            adDetailsList = new ArrayList(inputData.getAdDetailsList().size());
            adDetailsList.addAll(inputData.getAdDetailsList());
        } else if (MERCHANT.equals(envelope)) {
            userDetailsList = new ArrayList(inputData.getUserDetailsList().size());
            userDetailsList.addAll(inputData.getUserDetailsList());
        }

        //once both the lists are populated, peform the ad conversion rate calculation
        if(userDetailsList != null && adDetailsList != null && userDetailsList.size() > 0 && adDetailsList.size() > 0) {
            Double adConversionRate = getAdConversionRate(userDetailsList, adDetailsList);

            //send the ad conversion rate to merchant. use merchants public key to encrypt the message such that only merchant will be able to decrypt it
            byte[] encryptedReply = postOffice(clientToPublicKey.get(MERCHANT)).encryptMail(adConversionRate.toString().getBytes());
            postMail(encryptedReply, routingHintMap.get(MERCHANT));

            //send the ad conversion rate to service provider. use service providers public key to encrypt the message such that only merchant will be able to decrypt it
            encryptedReply = postOffice(clientToPublicKey.get(SERVICE_PROVIDER)).encryptMail(adConversionRate.toString().getBytes());
            postMail(encryptedReply, routingHintMap.get(SERVICE_PROVIDER));
        }
    }

    /**
     * Thsi calculates ad conversion rate.
     * ad conversion rate = users who have made purchases / total users who have clicked the ads
     * @param userDetailsList
     * @param adDetailsList
     * @return ad conversion rate
     */
    private Double getAdConversionRate(List<UserDetails> userDetailsList, List<AdDetails> adDetailsList) {

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

        return  (new Double(usersWhoPurchasedAfterClickingAd.size()) / new Double(adDetailsList.size()));
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