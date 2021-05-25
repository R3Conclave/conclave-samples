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

public class Host {

    public static void main(String args[]) throws EnclaveLoadException, IOException {
        final String enclaveClassName = "com.r3.conclave.sample.enclave.ReverseEnclave";

        //STEP 1 : Check if Platform supports enclave.

        try {
            EnclaveHost.checkPlatformSupportsEnclaves(true);
        } catch (EnclaveLoadException e) {
            e.printStackTrace();
        }

        //STEP 2 : Open a TCP server socket for any client who wishes to connect to the host
        ServerSocket acceptor = new ServerSocket(9999);
        Socket connection = acceptor.accept();
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

        //STEP 3 : Load and Start the Enclave
        EnclaveHost enclaveHost = EnclaveHost.load(enclaveClassName);

        enclaveHost.start(new AttestationParameters.DCAP() , mailCommands -> {
            for(MailCommand mailCommand  : mailCommands) {
                if(mailCommand instanceof MailCommand.PostMail) {
                    try {
                        sendBytesToClient(outputStream, ((MailCommand.PostMail) mailCommand).getEncryptedBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //STEP 4: Send attestation object to client
        byte[] enclaveInstanceInfo = enclaveHost.getEnclaveInstanceInfo().serialize();
        sendBytesToClient(outputStream, enclaveInstanceInfo);

        //STEP 5: Optional : Show how Host can connect locally directly to Enclave

        //STEP 6: Read the request mail from client
        DataInputStream input = new DataInputStream(connection.getInputStream());
        byte[] mailBytes = new byte[input.readInt()];
        input.readFully(mailBytes);

        //STEP 6: Relay Mail from Client to Enclave.

        enclaveHost.deliverMail(1, mailBytes, "routingHint");

        enclaveHost.close();
        outputStream.close();
    }

    private static void sendBytesToClient(DataOutputStream stream, byte[] bytes) throws IOException {
        stream.writeInt(bytes.length);
        stream.write(bytes);
        stream.flush();
    }
}
