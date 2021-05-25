package com.r3.conclave.sample.client;

import com.r3.conclave.client.EnclaveConstraint;
import com.r3.conclave.client.InvalidEnclaveException;
import com.r3.conclave.common.EnclaveInstanceInfo;
import com.r3.conclave.mail.Curve25519PrivateKey;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.mail.PostOffice;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.PrivateKey;

public class Client {

    public static void main(String args[]) throws InterruptedException, IOException, InvalidEnclaveException {

        //STEP 1: Take input from client
        if (args.length == 0) {
            System.err.println("Please pass the string to reverse on the command line using --args=\"String to Reverse\"");
            return;
        }
        String toReverse = String.join(" ", args);

        //STEP 2: Connect to Server TCP which is waiting for client requests
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

        //STEP 3: Retrieve the attestation object from Host immediately after connecting to Host
        byte[] attestationBytes = new byte[fromHost.readInt()];
        fromHost.readFully(attestationBytes);

        //STEP 4: Convert byte[] received from host to EnclaveInstanceInfo object
        EnclaveInstanceInfo instanceInfo = EnclaveInstanceInfo.deserialize(attestationBytes);

        //STEP 4: Verify the attestation against set of constraints
        //TODO

        //STEP 5: Create a dummy key pair for sending via mail to enclave
        PrivateKey key = Curve25519PrivateKey.random();

        //STEP 5: Create PostOffice specifying - clients public key, topic name , enclaves public key

        PostOffice postOffice = instanceInfo.createPostOffice(key, "topic");

        //STEP 6: Encrypt the message using enclave's public key

        byte[] encryptedRequest = postOffice.encryptMail(toReverse.getBytes());

        //STEP 7: Send the encrypted Mail to To Host to relay it to enclave
        toHost.writeInt(encryptedRequest.length);
        toHost.write(encryptedRequest);

        //STEP 8: Get the reply back from host via the socket
        byte[] encryptedReply = new byte[fromHost.readInt()];
        System.out.println("Reading reply mail of length " + encryptedReply.length + " bytes.");
        fromHost.readFully(encryptedReply);

        //STEP 8: Use Post Office to decrypt back the mail sent by the enclave
        EnclaveMail mail =  postOffice.decryptMail(encryptedReply);
        System.out.println("Reversed String on client side : " + new String(mail.getBodyAsBytes()));

        toHost.close();
        fromHost.close();
    }
}
