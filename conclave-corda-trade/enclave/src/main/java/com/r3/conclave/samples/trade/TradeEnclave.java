package com.r3.conclave.samples.trade;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.enclave.EnclavePostOffice;
import com.r3.conclave.mail.EnclaveMail;
import net.corda.samples.trade.common.ModelSerializer;
import net.corda.samples.trade.common.OrderModel;
import net.corda.samples.trade.common.TradeModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.PublicKey;
import java.util.*;

public class TradeEnclave extends Enclave {

    private Kryo kryo = new Kryo();
    private Map<String, Map<String, Set<OrderModel>>> instrumentMap = new HashMap<>();

    public TradeEnclave(){
        kryo.register(TradeModel.class, new ModelSerializer.TradeSerializer());
    }

    @Override
    protected void receiveMail(long id, EnclaveMail mail, String routingHint) {
        OrderModel order = readMail(mail);
        Map<String, Set<OrderModel>> orderMap;
        if(instrumentMap.get(order.getInstrument()) == null){
            Set<OrderModel> orders = new TreeSet<>();
            orderMap = new HashMap<>();
            orders.add(order);
            if(order.getTransactionType().equals("BUY")){
                orderMap.put("BUY", orders);
            }else{
                orderMap.put("SELL", orders);
            }
            instrumentMap.put(order.getInstrument(), orderMap);
        }else{
            orderMap = instrumentMap.get(order.getInstrument());
            matchOrder(order, orderMap);
        }
    }

//    public void testEnclave(OrderModel order){
//
//        Map<String, Set<OrderModel>> orderMap;
//        if(instrumentMap.get(order.getInstrument()) == null){
//            Set<OrderModel> orders = new TreeSet<>();
//            orderMap = new HashMap<>();
//            orders.add(order);
//            if(order.getTransactionType().equals("BUY")){
//                orderMap.put("BUY", orders);
//            }else{
//                orderMap.put("SELL", orders);
//            }
//            instrumentMap.put(order.getInstrument(), orderMap);
//        }else{
//            orderMap = instrumentMap.get(order.getInstrument());
//            matchOrder(order, orderMap);
//        }
//    }

    private void matchOrder(OrderModel thisOrder, Map<String, Set<OrderModel>> orderMap){
        List<OrderModel> matchedOrders = new ArrayList<>();

        // Current order is a buy order
        if(thisOrder.getTransactionType().equals("BUY")){
            if(orderMap.get("SELL") != null){
                Set<OrderModel> sellOrders = orderMap.get("SELL");
                Iterator<OrderModel> itr = sellOrders.iterator();
                int buyQty = thisOrder.getQuantity();
                while(itr.hasNext()) {
                    OrderModel sellOrder = itr.next();

                    // Price match found
                    if (sellOrder.getPrice() <= thisOrder.getPrice()) {
                        int sellQty = sellOrder.getQuantity() - sellOrder.getExecutedQuantity();
                        // Sell quantity available in current order is greater than buy quantity. Buy order will execute fully.
                        if (sellQty >= buyQty) {
                            sellQty = sellQty - buyQty;
                            sellOrder.setExecutedQuantity(sellOrder.getExecutedQuantity() + buyQty);
                            thisOrder.setExecutedQuantity(buyQty);
                            createAndSendTrade(buyQty, sellOrder.getPrice(), thisOrder.getInstrument(),
                                    thisOrder.getBroker(), sellOrder.getBroker(), thisOrder.getOrderId(),
                                    sellOrder.getOrderId());

                            // Sell order fully executed. Remove from pending orders.
                            if (sellQty == 0) {
                                sellOrders.remove(sellOrder);
                            }
                            break;
                        // Sell Quantity available in current order is greater than buy quantity.
                        } else {
                            buyQty = buyQty - sellQty;
                            sellOrder.setExecutedQuantity(sellOrder.getExecutedQuantity() + sellQty);
                            thisOrder.setExecutedQuantity(sellQty);
                            createAndSendTrade(sellQty, sellOrder.getPrice(), thisOrder.getInstrument(),
                                    thisOrder.getBroker(), sellOrder.getBroker(), thisOrder.getOrderId(),
                                    sellOrder.getOrderId());
                            sellOrders.remove(sellOrder);
                        }
                    // No Price match found. Add to pending orders
                    } else {
                        Set<OrderModel> buyOrders  = orderMap.get("BUY");
                        if(buyOrders ==null){
                            buyOrders = new TreeSet<OrderModel>();
                        }
                        buyOrders.add(thisOrder);
                        orderMap.put("BUY", buyOrders);
                        break;
                    }
                }

                // Current order partially executed. Add to pending orders
                if(thisOrder.getExecutedQuantity()<thisOrder.getQuantity()){
                    Set<OrderModel> buyOrders  = orderMap.get("BUY");
                    if(buyOrders ==null){
                        buyOrders = new TreeSet<OrderModel>();
                    }
                    buyOrders.add(thisOrder);
                    orderMap.put("BUY", buyOrders);
                }
            }else{
                // No corresponding sell orders available. Add to pending orders
                Set<OrderModel> buyOrders  = orderMap.get("BUY");
                if(buyOrders ==null){
                    buyOrders = new TreeSet<OrderModel>();
                }
                buyOrders.add(thisOrder);
                orderMap.put("BUY", buyOrders);
            }
        // Current order is a sell order
        }else{
            if(orderMap.get("BUY") != null){
                Set<OrderModel> buyOrders = orderMap.get("BUY");
                Iterator<OrderModel> itr = buyOrders.iterator();
                int sellQty = thisOrder.getQuantity();
                while(itr.hasNext()) {
                    OrderModel buyOrder = itr.next();

                    // Price match found
                    if (buyOrder.getPrice() >= thisOrder.getPrice()) {
                        int buyQty = buyOrder.getQuantity() - buyOrder.getExecutedQuantity();
                        // Buy quantity available in current order is greater than sell quantity. Buy order will execute fully.
                        if (buyQty >= sellQty) {
                            buyQty = buyQty - sellQty;
                            buyOrder.setExecutedQuantity(buyOrder.getExecutedQuantity() + sellQty);
                            thisOrder.setExecutedQuantity(sellQty);

                            createAndSendTrade(sellQty, buyOrder.getPrice(), thisOrder.getInstrument(),
                                    buyOrder.getBroker(), thisOrder.getBroker(), buyOrder.getOrderId(),
                                    thisOrder.getOrderId());
                            // Buy order fully executed. Remove from pending orders.
                            if (buyQty == 0) {
                                buyOrders.remove(buyOrder);
                            }
                            break;
                            // Buy Quantity available in current order is greater than sell quantity.
                        } else {
                            sellQty = sellQty - buyQty;
                            buyOrder.setExecutedQuantity(buyOrder.getExecutedQuantity() + buyQty);
                            thisOrder.setExecutedQuantity(buyQty);
                            createAndSendTrade(buyQty, buyOrder.getPrice(), thisOrder.getInstrument(),
                                    buyOrder.getBroker(), thisOrder.getBroker(), buyOrder.getOrderId(),
                                    thisOrder.getOrderId());
                            buyOrders.remove(buyOrder);
                        }
                        // No Price match found. Add to pending orders
                    } else {
                        Set<OrderModel> sellOrders  = orderMap.get("SELL");
                        if(sellOrders ==null){
                            sellOrders = new TreeSet<OrderModel>();
                        }
                        sellOrders.add(thisOrder);
                        orderMap.put("SELL", sellOrders);
                        break;
                    }
                }

                // Current order partially executed. Add to pending orders
                if(thisOrder.getExecutedQuantity()<thisOrder.getQuantity()){
                    Set<OrderModel> sellOrders  = orderMap.get("SELL");
                    if(sellOrders ==null){
                        sellOrders = new TreeSet<OrderModel>();
                    }
                    sellOrders.add(thisOrder);
                    orderMap.put("SELL", sellOrders);
                }
            }else{
                // No corresponding buy orders available. Add to pending orders
                Set<OrderModel> sellOrders  = orderMap.get("SELL");
                if(sellOrders ==null){
                    sellOrders = new TreeSet<OrderModel>();
                }
                sellOrders.add(thisOrder);
                orderMap.put("SELL", sellOrders);
            }
        }
    }

    private void createAndSendTrade(int quantity, double price, String instrument, String buyer, String seller,
                                    String buyOrderRef, String sellOrderRef){
        TradeModel trade = new TradeModel(UUID.randomUUID().toString(), buyOrderRef, sellOrderRef, quantity, instrument, price, buyer, seller);
        sendMail(trade);
    }

    private void sendMail(TradeModel trade) {
        Output output = serializeTrade(trade);
        postMail(output.getBuffer(), "");
    }



    private Output serializeTrade(TradeModel trade){
        Output output = new Output(new ByteArrayOutputStream());
        kryo.writeObject(output, trade);
        output.flush();
        output.close();
        return output;
    }


    private OrderModel readMail(EnclaveMail mail) {
        kryo.register(OrderModel.class, new ModelSerializer.OrderSerializer());
        Input input = new Input(new ByteArrayInputStream(mail.getBodyAsBytes()));
        return kryo.readObject(input, OrderModel.class);
    }

}
