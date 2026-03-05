package com.example.saga.common.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderPlacedPayload(
    UUID orderId,
    UUID customerId,
    BigDecimal totalAmount,
    String currency,
    List<OrderLinePayload> lines
) {
  public record OrderLinePayload(UUID productId, int qty) {
  }
}
