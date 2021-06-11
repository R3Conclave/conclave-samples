package com.r3.conclave.sample.host;

import com.r3.conclave.common.EnclaveInstanceInfo;
import com.r3.conclave.host.AttestationParameters;
import com.r3.conclave.host.EnclaveHost;
import com.r3.conclave.host.EnclaveLoadException;
import com.r3.conclave.host.MailCommand;

import java.net.ServerSocket;

import com.r3.conclave.host.MockOnlySupportedException;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * This class demonstrates how to load an enclave and exchange byte arrays with it.
 */
public class Host {
    private static final String ENCLAVE_CLASS_NAME =
            "com.r3.conclave.sample.enclave.DAEnclave";
    private EnclaveHost enclaveHost;

    public static void main(String[] args) throws EnclaveLoadException, IOException {
        // Report whether the platform supports hardware enclaves.
        //
        // This method will always check the hardware state regardless of whether running in Simulation,
        // Debug or Release mode. If the platform supports hardware enclaves then no exception is thrown.
        // If the platform does not support enclaves or requires enabling, an exception is thrown with the
        // details in the exception message.
        //
        // If the platform supports enabling of enclave support via software then passing true as a parameter
        // to this function will attempt to enable enclave support on the platform. Normally this process
        // will have to be run with root/admin privileges in order for it to be enabled successfully.


        // Enclaves get interesting when remote clients can talk to them.
        // Let's open a TCP socket and implement a trivial protocol that lets a remote client use it.
        // A real app would use SSL here to protect client/host communications, even though the only
        // data we're sending and receiving here is encrypted to the enclave: better safe than sorry.

        Host host = new Host();
        host.verifyPlatformSupport();
        host.startServer();
    }

    private void startServer() throws IOException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(5051);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw ioe;
        }
        System.out.println("Listening on port 5051");
        Socket clientSocket = null;
        try {
            assert serverSocket != null;
            clientSocket = serverSocket.accept();
            System.out.println("Connected with client");
            DataOutputStream outputToClient = new DataOutputStream(clientSocket.getOutputStream());
            System.out.println("Output stream for client");
            try {
                initializeEnclave(outputToClient);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

            final EnclaveInstanceInfo attestation = enclaveHost.getEnclaveInstanceInfo();
            final byte[] attestationBytes = attestation.serialize();
            sendMessageToClient(outputToClient, attestationBytes);
            recieveMailFromClientAndDeliverToEnclave(clientSocket);
        } catch (IOException e) {
            System.err.println("I/O error: " + e);
            throw e;
        }

    }

    private void recieveMailFromClientAndDeliverToEnclave(Socket clientSocket) {
        try {
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            byte[] mailBytes = new byte[input.readInt()];
            input.readFully(mailBytes);
            enclaveHost.deliverMail(1, mailBytes, "routingHint");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void sendMessageToClient(DataOutputStream outputToClient, byte[] content) {
        try {
            outputToClient.writeInt(content.length);
            outputToClient.write(content);
            outputToClient.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void verifyPlatformSupport() {
        try {
            EnclaveHost.checkPlatformSupportsEnclaves(true);
            System.out.println("This platform supports enclaves in simulation, debug and release mode.");
        } catch (EnclaveLoadException e) {
            System.err.println("This platform does not support hardware enclaves: " + e.getMessage());
        }
    }

    private void initializeEnclave(DataOutputStream outputToClient) throws EnclaveLoadException {
        enclaveHost = EnclaveHost.load(ENCLAVE_CLASS_NAME);
        try {

            enclaveHost.start(
                    new AttestationParameters.DCAP(), mailCommands -> {
                        for (MailCommand command : mailCommands) {
                            if (command instanceof MailCommand.PostMail) {
                                byte[] content = ((MailCommand.PostMail) command).getEncryptedBytes();
                                sendMessageToClient(outputToClient, content);
                            }
                        }
                    }
            );
        } catch (Exception e) {
            System.err.println("Error occurred while starting up the enclave " + e.getMessage());
        }


    }
}
