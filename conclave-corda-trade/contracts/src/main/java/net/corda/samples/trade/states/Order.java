package net.corda.samples.trade.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.trade.common.enums.Instrument;
import net.corda.samples.trade.common.enums.OrderStatus;
import net.corda.samples.trade.common.enums.TransactionType;
import net.corda.samples.trade.contracts.OrderContract;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@BelongsToContract(OrderContract.class)
public class Order implements ContractState, LinearState {
    private UniqueIdentifier orderId;
    private LocalDateTime timeStamp;
    private OrderStatus orderStatus;
    private TransactionType transactionType;
    private Instrument instrument;
    private Double price;
    private int quantity;
    private int executedQuantity;


    private Party broker;

    public Order(UniqueIdentifier orderId, LocalDateTime timeStamp, OrderStatus orderStatus,
                 TransactionType transactionType, Instrument instrument, Double price, int quantity, int executedQuantity,
                 Party broker) {
        this.orderId = orderId;
        this.timeStamp = timeStamp;
        this.orderStatus = orderStatus;
        this.transactionType = transactionType;
        this.instrument = instrument;
        this.price = price;
        this.quantity = quantity;
        this.executedQuantity = executedQuantity;
        this.broker = broker;
    }


    public UniqueIdentifier getOrderId() {
        return orderId;
    }


    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public Double getPrice() {
        return price;
    }

    public Party getBroker() {
        return broker;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getExecutedQuantity() {
        return executedQuantity;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(broker);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return orderId;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", timeStamp=" + timeStamp +
                ", orderStatus=" + orderStatus +
                ", transactionType=" + transactionType +
                ", instrument=" + instrument +
                ", price=" + price +
                ", quantity=" + quantity +
                ", executedQuantity=" + executedQuantity +
                ", broker=" + broker +
                '}';
    }
}