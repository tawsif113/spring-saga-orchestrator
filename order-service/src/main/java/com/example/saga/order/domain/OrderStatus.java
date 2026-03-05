package com.example.saga.order.domain;

public enum OrderStatus {
  CREATED,
  DRAFT,
  PLACED,
  CANCELLED,
  FAILED,
  CONFIRMED,
  INVENTORY_RESERVED,
  PAYMENT_AUTHORIZED
}
