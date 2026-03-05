package com.example.saga.common;

import java.util.UUID;

public final class Ids {
  private Ids() {}

  public record CustomerId(UUID value) {}
  public record ProductId(UUID value) {}
  public record OrderId(UUID value) {}
  public record PaymentId(UUID value) {}
  public record ReservationId(UUID value) {}
}
