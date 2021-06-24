package net.corda.samples.trade.common;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class TradeModel{
    private String tradeId;
    private String buyerOrderRef;
    private String sellerOrderRef;
    private int quantity;
    private String instrument;
    private double price;
    private String buyer;
    private String seller;

    public TradeModel(String tradeId, String buyerOrderRef, String sellerOrderRef, int quantity, String instrument, double price, String buyer, String seller) {
        this.tradeId = tradeId;
        this.buyerOrderRef = buyerOrderRef;
        this.sellerOrderRef = sellerOrderRef;
        this.quantity = quantity;
        this.instrument = instrument;
        this.price = price;
        this.buyer = buyer;
        this.seller = seller;
    }

    public String getBuyerOrderRef() {
        return buyerOrderRef;
    }

    public void setBuyerOrderRef(String buyerOrderRef) {
        this.buyerOrderRef = buyerOrderRef;
    }

    public String getSellerOrderRef() {
        return sellerOrderRef;
    }

    public void setSellerOrderRef(String sellerOrderRef) {
        this.sellerOrderRef = sellerOrderRef;
    }

    public String getBuyer() {
        return buyer;
    }

    public void setBuyer(String buyer) {
        this.buyer = buyer;
    }

    public String getSeller() {
        return seller;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

}
