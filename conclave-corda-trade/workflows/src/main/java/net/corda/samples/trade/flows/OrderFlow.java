package net.corda.samples.trade.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.trade.common.ModelSerializer;
import net.corda.samples.trade.common.OrderModel;
import net.corda.samples.trade.common.enums.Instrument;
import net.corda.samples.trade.common.enums.OrderStatus;
import net.corda.samples.trade.common.enums.TransactionType;
import net.corda.samples.trade.contracts.OrderContract;
import net.corda.samples.trade.flows.service.EnclaveClientHelper;
import net.corda.samples.trade.flows.service.TradeEnclaveService;
import net.corda.samples.trade.states.Order;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

/***
 * This flow is used to send an order to the exchange for execution. The order is recorded locally and its encrypted
 * form is sent to the exchange for processing. The exchange serves as a host for running the enclave which matches
 * the orders.
 */
public class OrderFlow {

    private OrderFlow() {}

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private Instrument instrument;
        private TransactionType transactionType;
        private int quantity;
        private double price;
        private Party exchange;

        public Initiator(String instrument, String transactionType, int quantity, double price, Party exchange) {
            this.instrument = Enum.valueOf(Instrument.class, instrument);
            this.transactionType = Enum.valueOf(TransactionType.class, transactionType);
            this.quantity = quantity;
            this.price = price;
            this.exchange = exchange;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party broker = getOurIdentity();

            Order order = new Order(
                    new UniqueIdentifier(null, UUID.randomUUID()),
                    LocalDateTime.now(),
                    OrderStatus.SEND_TO_EXCHANGE,
                    transactionType,
                    instrument,
                    price,
                    quantity,
                    0,
                    broker
                    );

            // The order state is converted to the order model dor sending to the enclave.
            // Instead of passing actual state objects to the enclave, we use a model object instead.
            OrderModel orderModel = new OrderModel(
                    order.getOrderId().toString(),
                    order.getTimeStamp(),
                    order.getTransactionType().toString(),
                    order.getInstrument().toString(),
                    order.getPrice(),
                    order.getQuantity(),
                    order.getExecutedQuantity(),
                    order.getBroker().toString()
            );

            FlowSession flowSession = initiateFlow(exchange);


            EnclaveClientHelper enclave = this.getServiceHub().cordaService(EnclaveClientHelper.class);
            // The enclave measurement must be passed as the constraint and the corresponding code to verify the
            // measurement must be uncommented in the EnclaveClientHelper. It has been left black for ease of development.
            enclave.start(flowSession, "");
            enclave.sendToEnclave(serializeOrder(orderModel));

            // Record the order locally.
            TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
            transactionBuilder.addOutputState(order);
            transactionBuilder.addCommand(new OrderContract.Commands.CreateOrder(), Arrays.asList(broker.getOwningKey()));

            transactionBuilder.verify(getServiceHub());
            SignedTransaction signedTransaction  = getServiceHub().signInitialTransaction(transactionBuilder);

            signedTransaction = subFlow(new FinalityFlow(signedTransaction, Collections.emptyList()));

            return signedTransaction;
        }

        private byte[] serializeOrder(OrderModel orderModel){
            Kryo kryo = new Kryo();
            Output output = new Output(new ByteArrayOutputStream());
            kryo.register(OrderModel.class, new ModelSerializer.OrderSerializer());
            kryo.writeObject(output, orderModel);
            output.flush();
            output.close();
            return output.getBuffer();
        }
    }


    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void> {
        private final FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // We get the service and thus, a handle to the enclave.
            final TradeEnclaveService enclave = this.getServiceHub().cordaService(TradeEnclaveService.class);

            // Send the other party the enclave identity (remote attestation) for verification.
            counterpartySession.send(enclave.getAttestationBytes());

            // Receive a mail, send it to the enclave, get a reply and send it back to the peer.
            recieveOrderFromBroker(enclave);

            return null;
        }

        @Suspendable
        private void recieveOrderFromBroker(TradeEnclaveService host) throws FlowException {
            // Other party sends us an encrypted mail.
            byte[] encryptedMail = counterpartySession.receive(byte[].class).unwrap(it -> it);
            // Deliver mail to enclave to reply.
            host.deliverMail(encryptedMail);
        }
    }

}
