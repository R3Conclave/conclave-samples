package net.corda.samples.trade.flows.service;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.conclave.common.EnclaveInstanceInfo;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.mail.MailDecryptionException;
import com.r3.conclave.mail.PostOffice;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowSession;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * A helper class that wraps some boilerplate for communicating with enclaves.
 */
@CordaService
public class EnclaveClientHelper extends SingletonSerializeAsToken {
    private FlowSession session;
    private String constraint;
    private PostOffice postOffice;

    public EnclaveClientHelper(@NotNull AppServiceHub serviceHub) {}

    @Suspendable
    public EnclaveClientHelper start(FlowSession session, String constraint) throws FlowException {
        this.session = session;
        this.constraint = constraint;
        if (postOffice != null)
            //throw new IllegalStateException("An EnclaveClientHelper may not be started more than once.");
            return this;


        // Read the enclave attestation from the peer.
        // In future, deserialization will be handled more automatically.
        EnclaveInstanceInfo attestation = session.receive(byte[].class).unwrap(EnclaveInstanceInfo::deserialize);

        // The key hash below (the hex string after 'S') is the public key version of sample_private_key.pem
        // In a real app you should remove the SEC:INSECURE part, of course.

        // Use below code to verify attestation before sending confidential information to the enclave.
        // Commented for development purpose.

//        try {
//            EnclaveConstraint.parse(constraint).check(attestation);
//        } catch (InvalidEnclaveException e) {
//            throw new FlowException(e);
//        }

        // This will create a post office with a random sender key and use the "default" topic.
        postOffice = attestation.createPostOffice();
        return this;
    }

    @Suspendable
    public void sendToEnclave(byte[] messageBytes) {
        if (postOffice == null)
            throw new IllegalStateException("You must call start() first.");
        // Create the encrypted message and send it. We'll use a temporary key for now, so the message is
        // effectively unauthenticated so the enclave won't know who sent it. Future samples will show how to
        // integrate with the Corda identity infrastructure.
        //
        session.send(postOffice.encryptMail(messageBytes));
    }

    @Suspendable
    @NotNull
    public byte[] receiveFromEnclave() throws FlowException {
        if (postOffice == null)
            throw new IllegalStateException("You must call start() first.");
            EnclaveMail reply = session.receive(byte[].class).unwrap((mail) -> {
                try {
                    return postOffice.decryptMail(mail);
                } catch (IOException | MailDecryptionException e) {
                    throw new FlowException(e);
                }
            });
            return reply.getBodyAsBytes();
    }
}
