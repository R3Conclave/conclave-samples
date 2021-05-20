package com.r3.conclave.sample.enclave;

import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.mail.EnclaveMail;

public class ReverseEnclave extends Enclave {

    byte[] previousResult;

    //STEP 1: Receive messages directly from host
    @Override
    protected byte[] receiveFromUntrustedHost(byte[] bytes) {
        return super.receiveFromUntrustedHost(bytes);
    }

    //STEP 2: Receive Mail sent by Client via Host
    @Override
    protected void receiveMail(long id, EnclaveMail mail, String routingHint) {

        //STEP 3: Perform Computation

        //STEP 4: Create PostOffice and encrypt the reply to be sent to client

        //STEP 5: Send the encrypted Mail back to client using routing hint

    }

    private static byte[] reverse(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        for (int i = input.length() - 1; i >= 0; i--)
            builder.append(input.charAt(i));
        return builder.toString().getBytes();
    }
}
