package net.corda.samples.trade.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.trade.common.enums.Instrument;
import net.corda.samples.trade.contracts.TradeContract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(TradeContract.class)
public class Trade implements ContractState {

    private UniqueIdentifier tradeId;
    private UniqueIdentifier buyerOrderRef;
    private UniqueIdentifier sellerOrderRef;
    private int quantity;
    private Instrument instrument;
    private double price;
    private Party buyer;
    private Party seller;
    private Party exchange;

    public Trade(UniqueIdentifier tradeId, UniqueIdentifier buyerOrderRef, UniqueIdentifier sellerOrderRef,
                 int quantity, Instrument instrument, double price, Party buyer, Party seller, Party exchange) {
        this.tradeId = tradeId;
        this.buyerOrderRef = buyerOrderRef;
        this.sellerOrderRef = sellerOrderRef;
        this.quantity = quantity;
        this.instrument = instrument;
        this.price = price;
        this.buyer = buyer;
        this.seller = seller;
        this.exchange = exchange;
    }

    public UniqueIdentifier getTradeId() {
        return tradeId;
    }

    public UniqueIdentifier getBuyerOrderRef() {
        return buyerOrderRef;
    }

    public UniqueIdentifier getSellerOrderRef() {
        return sellerOrderRef;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public double getPrice() {
        return price;
    }

    public Party getBuyer() {
        return buyer;
    }

    public Party getSeller() {
        return seller;
    }

    public Party getExchange() {
        return exchange;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(buyer, seller, exchange);
    }

    @Override
    public String toString() {
        return "Trade{" +
                "tradeId=" + tradeId +
                ", buyerOrderRef=" + buyerOrderRef +
                ", sellerOrderRef=" + sellerOrderRef +
                ", quantity=" + quantity +
                ", instrument=" + instrument +
                ", price=" + price +
                ", buyer=" + buyer +
                ", seller=" + seller +
                ", exchange=" + exchange +
                '}';
    }
}
