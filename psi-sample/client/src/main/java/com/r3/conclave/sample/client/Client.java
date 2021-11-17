package com.r3.conclave.sample.client;

import com.r3.conclave.client.EnclaveConstraint;
import com.r3.conclave.client.InvalidEnclaveException;
import com.r3.conclave.common.EnclaveInstanceInfo;
import com.r3.conclave.mail.Curve25519PrivateKey;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.mail.PostOffice;
import com.r3.conclave.sample.common.AdDetails;
import com.r3.conclave.sample.common.InputData;
import com.r3.conclave.sample.common.Role;
import com.r3.conclave.sample.common.UserDetails;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client can be of two types: Merchant or Service Provider
 * Merchant - who supplies list of users who have made a purchase
 * Service Providers - who supplies a list of users who have clicked on the ad
 * Both the clients send the lists to enclave, which calculates the ad conversion rate and sends it back to the clients
 */
public class Client {

    public static void main(String[] args) throws InterruptedException, IOException, InvalidEnclaveException {

        if (args.length == 0) {
            System.err.println("Please pass [MERCHANT/SERVICE-PROVIDER] [CONSTRAINT] followed by list of credit card numbers separated by spaces");
            return;
        }
        Role clientType = args[0].equals("MERCHANT") ? Role.MERCHANT : Role.SERVICE_PROVIDER;
        String constraint = args[1];

        //connect to host server
        DataInputStream fromHost;
        DataOutputStream toHost;
        while (true) {
            try {
                System.out.println("Attempting to connect to localhost:9999");
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), 9999), 5000);
                fromHost = new DataInputStream(socket.getInputStream());
                toHost = new DataOutputStream(socket.getOutputStream());
                break;
            } catch (Exception e) {
                System.err.println("Retrying: " + e.getMessage());
                Thread.sleep(2000);
            }
        }

        //take inputs from user
        InputData inputData = new InputData();
        List<UserDetails> userDetailsList = null;
        List<AdDetails> adDetailsList= null;

        inputData.setClientType(clientType);

        if(Role.MERCHANT.equals(clientType)) {
            userDetailsList = new ArrayList<>(args.length);
            for (int i =2; i< args.length; i++) {
                UserDetails userDetails = new UserDetails(args[i]);
                userDetailsList.add(userDetails);
            }
            inputData.setUserDetailsList(userDetailsList);

        } else if(Role.SERVICE_PROVIDER.equals(clientType)) {
            adDetailsList = new ArrayList<>(args.length);
            for (int i =2; i< args.length; i++) {
                AdDetails adDetails = new AdDetails(args[i]);
                adDetailsList.add(adDetails);
            }
            inputData.setAdDetailsList(adDetailsList);
        }

        //retrieve the attestation object from host immediately after connecting to host
        byte[] attestationBytes = new byte[fromHost.readInt()];
        fromHost.readFully(attestationBytes);

        //convert byte[] received from host to EnclaveInstanceInfo object
        EnclaveInstanceInfo instanceInfo = EnclaveInstanceInfo.deserialize(attestationBytes);

        //verify attestation received by enclave against the enclave code hash which we have
        EnclaveConstraint.parse("S:"+ constraint +" PROD:1 SEC:INSECURE" ).check(instanceInfo);

        //create a dummy key pair for sending via mail to enclave
        PrivateKey key = Curve25519PrivateKey.random();

        //create PostOffice specifying - clients public key, topic name , enclaves public key
        PostOffice postOffice = instanceInfo.createPostOffice(key, UUID.randomUUID().toString());

        //encrypt the message using enclave's public key
        byte[] encryptedRequest = postOffice.encryptMail(serialize(inputData));

        //send the encrypted mail to host to relay it to enclave
        toHost.writeInt(encryptedRequest.length);
        toHost.write(encryptedRequest);

        //get the reply back from host via the socket
        byte[] encryptedReply = new byte[fromHost.readInt()];
        fromHost.readFully(encryptedReply);

        //use Post Office to decrypt back the mail sent by the enclave
        EnclaveMail mail = postOffice.decryptMail(encryptedReply);

        System.out.println("Ad Conversion Rate : " + new String(mail.getBodyAsBytes()) +"%");

        toHost.close();
        fromHost.close();
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

}
