package net.corda.samples.trade.common;


import net.corda.core.serialization.CordaSerializable;

import java.time.LocalDateTime;

@CordaSerializable
public class OrderModel implements Comparable<OrderModel>{

    private String orderId;
    private LocalDateTime timeStamp;
    private String transactionType;
    private String instrument;
    private Double price;
    private int quantity;
    private int executedQuantity;
    private String broker;

    public OrderModel(String orderId, LocalDateTime timeStamp, String transactionType,
                      String  instrument, Double price, int quantity, int executedQuantity, String broker) {
        this.orderId = orderId;
        this.timeStamp = timeStamp;
        this.transactionType = transactionType;
        this.instrument = instrument;
        this.price = price;
        this.quantity = quantity;
        this.executedQuantity = executedQuantity;
        this.broker = broker;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getExecutedQuantity() {
        return executedQuantity;
    }

    public void setExecutedQuantity(int executedQuantity) {
        this.executedQuantity = executedQuantity;
    }

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    @Override
    public int compareTo(OrderModel o) {
        if(transactionType.equals("BUY")){
            if(price>o.price){
                return 1;
            }else if(price<o.price){
                return -1;
            }else{
                return 0;
            }
        }else if(transactionType.equals("SELL")){
            if(price>o.price){
                return -1;
            }else if(price<o.price){
                return 1;
            }else{
                return 0;
            }
        }
        return 0;
    }
}
