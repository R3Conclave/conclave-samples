package com.r3.conclave.samples.trade;

import com.r3.conclave.host.EnclaveLoadException;
import com.r3.conclave.testing.MockHost;
import net.corda.samples.trade.common.OrderModel;
import net.corda.samples.trade.states.Order;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class TradeEnclaveTest {

    @Test
    public void testEnclave() throws EnclaveLoadException {

        MockHost<TradeEnclave> mockHost = MockHost.loadMock(TradeEnclave.class);
        mockHost.start(null, null);
        TradeEnclave tradeEnclave = mockHost.getEnclave();

        OrderModel order1 = new OrderModel("1", LocalDateTime.now(), "BUY", "TCS",
                100.0,10, 0, "B1");
        OrderModel order2 = new OrderModel("2", LocalDateTime.now(), "SELL", "TCS",
                100.0,100, 0, "B2");

        OrderModel order3 = new OrderModel("3", LocalDateTime.now(), "BUY", "TCS",
                100.0,15, 0, "B1");

        OrderModel order4 = new OrderModel("4", LocalDateTime.now(), "BUY", "TCS",
                100.0,200, 0, "B1");

//        tradeEnclave.testEnclave(order1);
//        tradeEnclave.testEnclave(order2);
//        tradeEnclave.testEnclave(order3);
//        tradeEnclave.testEnclave(order4);

    }
}
