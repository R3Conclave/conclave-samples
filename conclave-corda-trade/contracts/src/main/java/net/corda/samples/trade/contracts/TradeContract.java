package net.corda.samples.trade.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class TradeContract implements Contract {

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Contract verification logic goes here.
    }

    public interface Commands extends CommandData {
        class CreateTrade implements Commands {}
    }
}
