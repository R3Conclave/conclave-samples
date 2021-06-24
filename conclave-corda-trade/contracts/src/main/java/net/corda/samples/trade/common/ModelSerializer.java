package net.corda.samples.trade.common;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ModelSerializer {

    public static class OrderSerializer extends Serializer<OrderModel> {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

        @Override
        public void write(Kryo kryo, Output output, OrderModel order) {
            output.writeString(order.getOrderId());
            output.writeString(order.getTimeStamp().format(formatter));
            output.writeString(order.getTransactionType().toString());
            output.writeString(order.getInstrument().toString());
            output.writeDouble(order.getPrice());
            output.writeInt(order.getQuantity());
            output.writeInt(order.getExecutedQuantity());
            output.writeString(order.getBroker());
        }

        @Override
        public OrderModel read(Kryo kryo, Input input, Class<OrderModel> type) {
            String uniqueIdentifier = input.readString();
            String timeStamp = input.readString();
            String transactionType = input.readString();
            String instrument = input.readString();
            Double price = input.readDouble();
            int quantity = input.readInt();
            int executedQuantity = input.readInt();
            String broker = input.readString();

            OrderModel order = new OrderModel(uniqueIdentifier, LocalDateTime.parse(timeStamp, formatter),
                    transactionType, instrument, price, quantity, executedQuantity, broker);
            return order;
        }
    }

    public static class TradeSerializer extends Serializer<TradeModel> {

        @Override
        public void write(Kryo kryo, Output output, TradeModel trade) {
            output.writeString(trade.getTradeId());
            output.writeString(trade.getBuyerOrderRef());
            output.writeString(trade.getSellerOrderRef());
            output.writeInt(trade.getQuantity());
            output.writeString(trade.getInstrument());
            output.writeDouble(trade.getPrice());
            output.writeString(trade.getBuyer());
            output.writeString(trade.getSeller());
        }

        @Override
        public TradeModel read(Kryo kryo, Input input, Class<TradeModel> type) {
            String uuid = input.readString();
            String buyerOrderRef = input.readString();
            String sellerOrderRef = input.readString();
            int quantity = input.readInt();
            String instrument = input.readString();
            Double price = input.readDouble();
            String buyer = input.readString();
            String seller = input.readString();

            TradeModel tradeModel = new TradeModel(uuid, buyerOrderRef, sellerOrderRef, quantity,
                    instrument, price, buyer, seller);
            return tradeModel;
        }
    }
}
