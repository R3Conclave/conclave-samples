package net.corda.samples.trade.flows.service;

import com.r3.conclave.host.AttestationParameters;
import com.r3.conclave.host.EnclaveHost;
import com.r3.conclave.host.EnclaveLoadException;
import com.r3.conclave.host.MailCommand;
import com.r3.conclave.mail.MailDecryptionException;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.samples.trade.flows.TradeFlow;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

@CordaService
public class TradeEnclaveService extends SingletonSerializeAsToken {
    private Log log = LogFactory.getLog(TradeEnclaveService.class);
    private AppServiceHub serviceHub;
    private final EnclaveHost enclave;
    private final byte[] attestationBytes;
    private final AtomicInteger counter = new AtomicInteger();

    public TradeEnclaveService(@NotNull AppServiceHub serviceHub) {
        this.serviceHub = serviceHub;

        try {
            enclave = EnclaveHost.load("com.r3.conclave.samples.trade.TradeEnclave");
            // If you want to use pre-DCAP hardware via the older EPID protocol, you'll need to get the relevant API
            // keys from Intel and replace AttestationParameters.DCAP with AttestationParameters.EPID.
            enclave.start(new AttestationParameters.DCAP(), null, null, (commands) -> {
                // The enclave is requesting that we deliver messages transactionally. In Corda there's no way to
                // do an all-or-nothing message delivery to multiple peers at once: for that you need a genuine
                // ledger transaction which is more complex and slower. So for now we'll just deliver messages
                // non-transactionally.
                for (MailCommand command : commands) {
                    if (command instanceof MailCommand.PostMail) {
                        MailCommand.PostMail post = (MailCommand.PostMail) command;
                        enclaveToFlow(post.getEncryptedBytes());
                    }
                }
            });

            // The attestation data must be provided to the client of the enclave, via whatever mechanism you like.
            attestationBytes = enclave.getEnclaveInstanceInfo().serialize();
        } catch (EnclaveLoadException e) {
            throw new RuntimeException(e);   // Propagate and let the node abort startup, as this shouldn't happen.
        }

    }

    protected void enclaveToFlow(byte[] encryptedBytes) {
        // Start trade flow in a separate thread
        OrderResponse orderResponse = new OrderResponse(encryptedBytes);
        Thread t = new Thread(orderResponse);
        t.start();
    }

    public void deliverMail(byte[] encryptedMail) throws MailDecryptionException {
        enclave.deliverMail(encryptedMail, null);

    }

    public byte[] getAttestationBytes() {
        return attestationBytes;
    }

    class OrderResponse implements Runnable {

        private byte[] mailBytes;

        public OrderResponse(byte[] mailBytes) {
            this.mailBytes = mailBytes;
        }

        public void run() {
               serviceHub.startFlow(new TradeFlow.Initiator(mailBytes));
        }
    }

}
