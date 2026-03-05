package com.example.saga.common.events;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderInventoryReservedPayload(
    UUID orderId,
    BigDecimal totalAmount,
    String currency
) {
}
