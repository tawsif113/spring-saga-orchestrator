package com.example.saga.common.events;

public final class OrderEventTypes {
  public static final String ORDER_PLACED = "order.placed";
  public static final String ORDER_INVENTORY_RESERVED = "order.inventory-reserved";
  public static final String ORDER_PAYMENT_AUTHORIZED = "order.payment-authorized";
  public static final String ORDER_CONFIRMED = "order.confirmed";
  public static final String ORDER_FAILED = "order.failed";

  private OrderEventTypes() {
  }
}
