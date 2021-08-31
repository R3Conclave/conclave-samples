package net.corda.samples.trade.flows.service;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.samples.trade.flows.OrderUpdateFlow;
import net.corda.samples.trade.states.Trade;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Corda Service used to update corresponding orders once a trade has been received and recorded in the ledger.
 */
@CordaService
public class OrderUpdateService extends SingletonSerializeAsToken {

    private Log log = LogFactory.getLog(TradeEnclaveService.class);
    private AppServiceHub serviceHub;

    public OrderUpdateService(AppServiceHub appServiceHub) {
        this.serviceHub = appServiceHub;
        trackTradeUpdate();
    }

    private void trackTradeUpdate(){
        Party self = serviceHub.getMyInfo().getLegalIdentities().get(0);
        serviceHub.getVaultService().trackBy(Trade.class).getUpdates().subscribe(tradeUpdate -> {
            tradeUpdate.getProduced().forEach(tradeStateAndRef -> {
                Trade trade = tradeStateAndRef.getState().getData();
                if(trade.getBuyer().equals(self)){
                    OrderUpdate orderUpdate = new OrderUpdate(trade.getBuyerOrderRef(), trade.getQuantity());
                    Thread t = new Thread(orderUpdate);
                    t.start();
                }else if(trade.getSeller().equals(self)){
                    OrderUpdate orderUpdate = new OrderUpdate(trade.getSellerOrderRef(), trade.getQuantity());
                    Thread t = new Thread(orderUpdate);
                    t.start();
                }
            });
        });
    }

    class OrderUpdate implements Runnable {

        private UniqueIdentifier orderRef;
        private int quantity;

        public OrderUpdate(UniqueIdentifier orderRef, int quantity) {
            this.orderRef = orderRef;
            this.quantity = quantity;
        }

        public void run() {
            serviceHub.startFlow(new OrderUpdateFlow(orderRef, quantity));
        }
    }




}
