package com.example.saga.order.domain;

import com.example.saga.common.Money;
import com.example.saga.common.Ids.ProductId;

import java.util.Objects;

final class OrderLine {
  private final ProductId productId;
  private int qty;
  private final Money unitPrice;

  OrderLine(ProductId productId, int qty, Money unitPrice) {
    this.productId = Objects.requireNonNull(productId);
    this.qty = qty;
    this.unitPrice = Objects.requireNonNull(unitPrice);
  }

  ProductId productId() { return productId; }
  int qty() { return qty; }
  Money unitPrice() { return unitPrice; }

  void increaseQty(int add) { this.qty += add; }
}
