package com.r3.conclave.sample.auction.client;

import com.r3.conclave.client.EnclaveClient;
import com.r3.conclave.client.web.WebEnclaveTransport;
import com.r3.conclave.common.EnclaveConstraint;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.auction.common.Message;

import java.io.*;

/**
 * Simple client that communicates with the AuctionEnclave using the web host.
 */
public class AuctionClient {

    /** Client role to indicate bidder/ admin **/
    private static String roleType;

    /* Bid amount submitted by bidder */
    private static int bid = 0;

    /** URL of the web host running the enclave. **/
    private static String url;

    /** Enclave constraint which determines the enclave's identity and whether it's acceptable to use. **/
    private static EnclaveConstraint constraint;

    public static void  main(String[] args) throws Exception {
        roleType = args[0];
        constraint = EnclaveConstraint.parse(args[1]);
        url = args[2];
        if(roleType.equals("BIDDER")) {
            bid = Integer.parseInt(args[3]);
        }


        EnclaveClient enclaveClient = new EnclaveClient(constraint);

        try (WebEnclaveTransport transport = new WebEnclaveTransport(url);
             EnclaveClient client = enclaveClient)
        {
            client.start(transport);
            byte[] serializedOutput = serializeMessage(roleType, bid);

            // Send mail to enclave and receive response
            if(bid>0) {
                System.out.println("Sending a bid of " + bid + " to enclave");
            }else{
                System.out.println("Requesting Auction Result..");
            }
            EnclaveMail responseMail = client.sendMail(serializedOutput);

            //ResponseMail is null till enclave doesn't reply back to the client
            if (responseMail == null) {
                do {
                    Thread.sleep(2000);
                    //Poll for reply to enclave
                    responseMail = client.pollMail();
                } while (responseMail == null);
            }
            System.out.println("Bid Result : " + new String(responseMail.getBodyAsBytes()));
        }
    }

    public static byte[] serializeMessage(String messageType, int bid) {
        Message message = new Message(messageType, bid);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os;
        try {
            os = new ObjectOutputStream(out);
            os.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }


}