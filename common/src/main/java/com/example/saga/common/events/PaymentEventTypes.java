package com.example.saga.common.events;

public final class PaymentEventTypes {
  public static final String PAYMENT_AUTHORIZED = "payment.authorized";
  public static final String PAYMENT_REJECTED = "payment.rejected";

  private PaymentEventTypes() {
  }
}
