package com.r3.conclave.sample.enclave;

import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.mail.EnclaveMail;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * Simply reverses the bytes that are passed in.
 */
public class ReverseEnclave extends Enclave {
    // We store the previous result to showcase that the enclave internals can be examined in a mock test.
    byte[] previousResult;
    private final Context context;
    private final Value bindings;
    private static final String pythonCode = "def reverse(input):\n"
            + " return input[::-1]";

    public ReverseEnclave() {
        context = Context.create("python");
        bindings = context.getBindings("python");
        context.eval("python", pythonCode);
    }

    @Override
    protected byte[] receiveFromUntrustedHost(byte[] bytes) {
        // This is used for host->enclave calls so we don't have to think about authentication.
        final String input = new String(bytes);
        byte[] result = reverse(input).getBytes();
        previousResult = result;
        return result;
    }

    private String reverse(String input) {
        Value result = bindings.getMember("reverse").execute(input);
        return result.asString();
    }

    @Override
    protected void receiveMail(EnclaveMail mail, String routingHint) {
        // This is used when the host delivers a message from the client.
        // First, decode mail body as a String.
        final String stringToReverse = new String(mail.getBodyAsBytes());
        // Reverse it and re-encode to UTF-8 to send back.
        final byte[] reversedEncodedString = reverse(stringToReverse).getBytes();
        // Get the post office object for responding back to this mail and use it to encrypt our response.
        final byte[] responseBytes = postOffice(mail).encryptMail(reversedEncodedString);
        postMail(responseBytes, routingHint);
    }
}
