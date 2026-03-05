package com.example.saga.common.events;

import java.util.UUID;
import java.util.List;

public record OrderConfirmedPayload(UUID orderId, List<OrderLinePayload> lines) {
  public record OrderLinePayload(UUID productId, int qty) {
  }
}
