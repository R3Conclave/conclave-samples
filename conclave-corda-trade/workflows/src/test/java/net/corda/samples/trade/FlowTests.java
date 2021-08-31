package net.corda.samples.trade;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.node.NetworkParameters;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.trade.flows.OrderFlow;
import net.corda.samples.trade.flows.TradeFlow;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Collections;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(
                new MockNetworkParameters(
                        ImmutableList.of(
                                TestCordapp.findCordapp("net.corda.samples.trade.flows"),
                                TestCordapp.findCordapp("net.corda.samples.trade.contracts")
                        )
                ).withNetworkParameters(new NetworkParameters(4, Collections.emptyList(),
                        10485760, 10485760 * 50, Instant.now(), 1,
                        Collections.emptyMap()))
        );
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void testCreateAssetFlow() throws Exception {
        OrderFlow.Initiator flow = new OrderFlow.Initiator("TCS", "BUY", 1000, 100, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
    }

}