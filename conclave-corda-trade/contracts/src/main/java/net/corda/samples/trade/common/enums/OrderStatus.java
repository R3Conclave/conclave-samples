package net.corda.samples.trade.common.enums;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public enum OrderStatus {
    CANCELLED,
    SEND_TO_EXCHANGE,
    EXECUTED,
    PARTIALLY_EXECUTED
}
