package com.example.saga.inventory.domain;

import com.example.saga.common.Ids.ProductId;

import java.util.Objects;

public class StockItem { // Aggregate Root
  private final ProductId productId;
  private int availableQty;
  private int reservedQty;

  public StockItem(ProductId productId) {
    this(productId, 0, 0);
  }

  private StockItem(ProductId productId, int availableQty, int reservedQty) {
    this.productId = Objects.requireNonNull(productId);
    this.availableQty = availableQty;
    this.reservedQty = reservedQty;
    validateState();
  }

  public static StockItem rehydrate(ProductId productId, int availableQty, int reservedQty) {
    return new StockItem(productId, availableQty, reservedQty);
  }

  public ProductId productId() {
    return productId;
  }

  public int availableQty() {
    return availableQty;
  }

  public int reservedQty() {
    return reservedQty;
  }

  public void addStock(int qty) {
    ensurePositive(qty, "qty must be > 0");
    availableQty += qty;
    validateState();
  }

  public void reserve(int qty) {
    ensurePositive(qty, "qty must be > 0");
    if (availableQty < qty) {
      throw new IllegalStateException("Not enough available stock");
    }
    availableQty -= qty;
    reservedQty += qty;
    validateState();
  }


  public void commitReservation(int qty) {
    ensurePositive(qty, "qty must be > 0");
    if (reservedQty < qty) {
      throw new IllegalStateException("Cannot commit more than reserved stock");
    }
    reservedQty -= qty;
    validateState();
  }

  public void release(int qty) {
    ensurePositive(qty, "qty must be > 0");
    if (reservedQty < qty) {
      throw new IllegalStateException("Cannot release more than reserved stock");
    }
    reservedQty -= qty;
    availableQty += qty;
    validateState();
  }

  private void validateState() {
    if (availableQty < 0) throw new IllegalStateException("availableQty cannot be negative");
    if (reservedQty < 0) throw new IllegalStateException("reservedQty cannot be negative");
  }

  private static void ensurePositive(int qty, String message) {
    if (qty <= 0) throw new IllegalArgumentException(message);
  }
}