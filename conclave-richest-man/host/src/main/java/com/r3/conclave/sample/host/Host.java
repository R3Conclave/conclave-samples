package com.r3.conclave.sample.host;

import com.r3.conclave.common.EnclaveInstanceInfo;
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

public class Host {

    private static final String ENCLAVE_CLASS_NAME =
            "com.r3.conclave.sample.enclave.RichestPersonEnclave";
    private EnclaveHost enclaveHost;
    private Map<String, Socket> clientMap = new HashMap<>();

    public static void main(String[] args) throws EnclaveLoadException, IOException {
        Host host = new Host();
        host.verifyPlatformSupport();
        host.initializeEnclave();
        host.startServer();
    }

    private void startServer() throws IOException{
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(5051);
        }catch (IOException ioe){
            ioe.printStackTrace();
            throw ioe;
        }
        System.out.println("Listening on port 5051");
        while (true) {
            Socket clientSocket = null;
            try {
                assert serverSocket != null;
                clientSocket = serverSocket.accept();

                String routingHint = UUID.randomUUID().toString();
                clientMap.put(routingHint, clientSocket);

                final EnclaveInstanceInfo attestation = enclaveHost.getEnclaveInstanceInfo();
                final byte[] attestationBytes = attestation.serialize();
                sendMessageToClient(routingHint, attestationBytes);
                recieveMailFromClientAndDeliverToEnclave(clientSocket, routingHint);
            } catch (IOException e) {
                System.out.println("I/O error: " + e);
                throw e;
            }
        }
    }

    private void sendMessageToClient(String routingHint, byte[] content){
        try {
            Socket clientSocket = clientMap.get(routingHint);
            DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());
            outputStream.writeInt(content.length);
            outputStream.write(content);
            outputStream.flush();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }


    private void recieveMailFromClientAndDeliverToEnclave(Socket clientSocket, String routingHint){
        try {
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            byte[] mailBytes = new byte[input.readInt()];
            input.readFully(mailBytes);

            enclaveHost.deliverMail(1, mailBytes, routingHint);
        }catch (IOException ioException){
            ioException.printStackTrace();
        }
    }

    private  void verifyPlatformSupport(){
        try {
            EnclaveHost.checkPlatformSupportsEnclaves(true);
            System.out.println("This platform supports enclaves in simulation, debug and release mode.");
        } catch (EnclaveLoadException e) {
            System.out.println("This platform does not support hardware enclaves: " + e.getMessage());
        }
    }

    private void initializeEnclave() throws EnclaveLoadException{
        enclaveHost = EnclaveHost.load(ENCLAVE_CLASS_NAME);
        enclaveHost.start(
                new AttestationParameters.DCAP(), mailCommands -> {
                    for (MailCommand command : mailCommands) {
                        if (command instanceof MailCommand.PostMail) {
                            String routingHint = ((MailCommand.PostMail) command).getRoutingHint();
                            byte[] content = ((MailCommand.PostMail) command).getEncryptedBytes();
                            sendMessageToClient(routingHint, content);
                        }
                    }
                }
        );
    }

}
