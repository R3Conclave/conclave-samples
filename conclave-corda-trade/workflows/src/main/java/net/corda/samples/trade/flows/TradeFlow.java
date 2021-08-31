package net.corda.samples.trade.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.trade.common.ModelSerializer;
import net.corda.samples.trade.common.TradeModel;
import net.corda.samples.trade.common.enums.Instrument;
import net.corda.samples.trade.contracts.TradeContract;
import net.corda.samples.trade.states.Trade;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * This flow creates a transaction to send a trade to the participants once it has been matched by the enclave.
 * This flow is triggered from the TradeEnclave service ( at the host), when it receives a trade as a mail from
 * the enclave.
 */
public class TradeFlow {

    private TradeFlow() {}

    @InitiatingFlow
    @StartableByService
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private byte[] tradeBytes;

        public Initiator(byte[] tradeBytes) {
            this.tradeBytes = tradeBytes;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            TradeModel model = deSerializeTrade(tradeBytes);

            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party exchange = getOurIdentity();

            Party buyer = getServiceHub().getNetworkMapCache().getPeerByLegalName(CordaX500Name.parse(model.getBuyer()));
            Party seller = getServiceHub().getNetworkMapCache().getPeerByLegalName(CordaX500Name.parse(model.getSeller()));
            Trade trade = new Trade(
                    new UniqueIdentifier(null, UUID.fromString(model.getTradeId())),
                    new UniqueIdentifier(null, UUID.fromString(model.getBuyerOrderRef())),
                    new UniqueIdentifier(null, UUID.fromString(model.getSellerOrderRef())),
                    model.getQuantity(),
                    Enum.valueOf(Instrument.class, model.getInstrument()),
                    model.getPrice(),
                    buyer,
                    seller,
                    exchange
            );

            TransactionBuilder builder = new TransactionBuilder(notary)
                    .addOutputState(trade)
                    .addCommand(new TradeContract.Commands.CreateTrade(), Arrays.asList(exchange.getOwningKey()));

            builder.verify(getServiceHub());
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(builder);

            FlowSession buyerSession = initiateFlow(buyer);
            FlowSession sellerSession = initiateFlow(seller);

            return subFlow(new FinalityFlow(signedTransaction, Arrays.asList(buyerSession, sellerSession)));
        }

        private TradeModel deSerializeTrade(byte[] tradeBytes){
            Kryo kryo = new Kryo();
            kryo.register(TradeModel.class, new ModelSerializer.TradeSerializer());
            Input input = new Input(new ByteArrayInputStream(tradeBytes));
            TradeModel tradeModel = kryo.readObject(input, TradeModel.class);
            return tradeModel;
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void> {

        private FlowSession cpSession;

        public Responder(FlowSession cpSession) {
            this.cpSession = cpSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new ReceiveFinalityFlow(cpSession));
            return null;
        }

    }
}
