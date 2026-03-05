package com.example.saga.common.events;

import java.util.UUID;
import java.util.List;

public record OrderFailedPayload(UUID orderId, String reason, List<OrderLinePayload> lines) {
  public record OrderLinePayload(UUID productId, int qty) {
  }
}
