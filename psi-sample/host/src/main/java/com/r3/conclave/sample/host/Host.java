package com.r3.conclave.sample.host;

import com.r3.conclave.host.AttestationParameters;
import com.r3.conclave.host.EnclaveHost;
import com.r3.conclave.host.EnclaveLoadException;
import com.r3.conclave.host.MailCommand;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Host accepts client connections, and relays messages from clients to enclaves. It also relays replies back from enclave to clients.
 */
public class Host {

    private static Map<String, Socket> clientMap = new HashMap<>();

    public static void main(String args[]) throws EnclaveLoadException, IOException {
        final String enclaveClassName = "com.r3.conclave.sample.enclave.PSIEnclave";

        try {
            EnclaveHost.checkPlatformSupportsEnclaves(true);
        } catch (EnclaveLoadException e) {
            System.out.println("Host does not support SGX");
        }

        EnclaveHost enclaveHost = EnclaveHost.load(enclaveClassName);

        //open a TCP server socket for any client who wishes to connect to the host
        ServerSocket acceptor = new ServerSocket(9999);
        acceptor.setSoTimeout(60000);
        System.out.println("Listening on port 9999");

        AtomicBoolean loop = new AtomicBoolean(true);

        while (loop.get() == true) {
            //accept is a blocking call and will wait for any client to connect
            Socket clientSocket = acceptor.accept();

            //routing hint identifies a particular client. This is used by enclave to send reply back to client
            String routingHint = UUID.randomUUID().toString();

            //clientMap used to maintain routing hint and client socket.
            //This can be used by the host to send reply back to a particular client socket using routing hint
            clientMap.put(routingHint, clientSocket);

            //this registers a callback which gets called when enclave wants to send a reply back to client
            //for this sample, this callback is triggered when the merchant and service provider both
            //provide their input data
            enclaveHost.start(new AttestationParameters.DCAP() , mailCommands -> {
                for(MailCommand mailCommand : mailCommands) {
                    if(mailCommand instanceof MailCommand.PostMail) {
                        try {
                            loop.set(false);

                            System.out.println("Host : Sending message to client");

                            MailCommand.PostMail postMail = (MailCommand.PostMail) mailCommand;

                            sendBytesToClient(postMail.getRoutingHint()
                                    , postMail.getEncryptedBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            //retrieve the remote attestation object and send it to client for verification
            byte[] enclaveInstanceInfo = enclaveHost.getEnclaveInstanceInfo().serialize();
            sendBytesToClient(routingHint, enclaveInstanceInfo);

            //get the request from client
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            byte[] mailBytes = new byte[input.readInt()];
            input.readFully(mailBytes);

            //forward the request to enclave. The request is encrypted using enclave's public
            //key and hence is not accessible to host
            enclaveHost.deliverMail(1, mailBytes, routingHint);
        }


    }

    /**
     * Used to send reply back to client using routing hint and client socket stored in clientMap
     */
    private static void sendBytesToClient(String routingHint, byte[] bytes) throws IOException {
        Socket clientSocket = clientMap.get(routingHint);
        DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());
        outputStream.writeInt(bytes.length);
        outputStream.write(bytes);
        outputStream.flush();
    }
}
