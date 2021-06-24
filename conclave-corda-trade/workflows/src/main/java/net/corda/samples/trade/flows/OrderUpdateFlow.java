package net.corda.samples.trade.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByService;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.trade.common.enums.OrderStatus;
import net.corda.samples.trade.contracts.OrderContract;
import net.corda.samples.trade.states.Order;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The flow is used update the orders when a trade has been matched.
 * This flow is triggered from the OrderUpdateService.
 */
@StartableByService
public class OrderUpdateFlow extends FlowLogic<SignedTransaction> {

    private UniqueIdentifier orderRef;
    private int tradedQuantity;

    public OrderUpdateFlow(UniqueIdentifier orderRef, int tradedQuantity) {
        this.orderRef = orderRef;
        this.tradedQuantity = tradedQuantity;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        StateAndRef<Order> orderStateAndRef = getInput(orderRef);
        Order input = orderStateAndRef.getState().getData();

        OrderStatus orderStatus = null;

        /* Mark the order as fully executed if all quantity has been matched. For partial
           mark order sa partially executed. */
        if(input.getQuantity() == input.getExecutedQuantity() + tradedQuantity){
            orderStatus = OrderStatus.EXECUTED;
        }else{
            orderStatus = OrderStatus.PARTIALLY_EXECUTED;
        }

        Order output = new Order(
            input.getOrderId(),
            input.getTimeStamp(),
            orderStatus,
            input.getTransactionType(),
            input.getInstrument(),
            input.getPrice(),
            input.getQuantity(),
            input.getExecutedQuantity() + tradedQuantity,
            input.getBroker()
        );

        TransactionBuilder builder = new TransactionBuilder(notary)
                .addInputState(orderStateAndRef)
                .addOutputState(output)
                .addCommand(new OrderContract.Commands.UpdateOrder(), Arrays.asList(getOurIdentity().getOwningKey()));
        builder.verify(getServiceHub());
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(builder);
        return  subFlow(new FinalityFlow(signedTransaction, Collections.emptyList()));
    }

    /**
     * Finds the corresponding order state based on the order identifier passed.
     * @param identifier
     * @return
     */
    private StateAndRef<Order> getInput(UniqueIdentifier identifier){
        List<StateAndRef<Order>> orderStateAndRef = getServiceHub().getVaultService().queryBy(Order.class).getStates();

        StateAndRef<Order> inputStateAndRefs0 = orderStateAndRef.stream().filter(orderStateAndRef1 -> {
            Order order = orderStateAndRef1.getState().getData();
            return order.getOrderId().equals(identifier);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Order Not Found"));
        return inputStateAndRefs0;
    }
}
