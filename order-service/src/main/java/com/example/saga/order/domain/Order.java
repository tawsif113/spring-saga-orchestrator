package com.example.saga.order.domain;

import com.example.saga.common.Address;
import com.example.saga.common.Ids.CustomerId;
import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.Ids.ProductId;
import com.example.saga.common.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Order {

  private final OrderId id;
  private final CustomerId customerId;
  private final Address shipTo;          // Value Object snapshot
  private OrderStatus status = OrderStatus.DRAFT;

  // Entities INSIDE the aggregate (not exposed directly)
  private final List<OrderLine> lines = new ArrayList<>();

  public Order(OrderId id, CustomerId customerId, Address shipTo) {
    this.id = Objects.requireNonNull(id);
    this.customerId = Objects.requireNonNull(customerId);
    this.shipTo = Objects.requireNonNull(shipTo);
  }

  public OrderId id() { return id; }
  public CustomerId customerId() { return customerId; }
  public Address shipTo() { return shipTo; }
  public OrderStatus status() { return status; }

  // Read-only snapshots so outside code doesn't touch internal entities
  public List<LineSnapshot> lineSnapshots() {
    return lines.stream().map(l -> new LineSnapshot(l.productId(), l.qty(), l.unitPrice())).toList();
  }

  public Money total() {
    Money sum = new Money(BigDecimal.ZERO, "BDT");
    for (var l : lines) sum = sum.add(l.unitPrice().multiply(l.qty()));
    return sum;
  }

  public void addLine(ProductId productId, int qty, Money unitPrice) {
    ensure(status == OrderStatus.DRAFT, "Can only edit an order in DRAFT");
    Objects.requireNonNull(productId);
    Objects.requireNonNull(unitPrice);
    if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");

    // invariant: same product appears once (merge qty)
    var existing = lines.stream().filter(x -> x.productId().equals(productId)).findFirst();
    if (existing.isPresent()) {
      existing.get().increaseQty(qty);
    } else {
      lines.add(new OrderLine(productId, qty, unitPrice));
    }
  }

  public void place() {
    ensure(status == OrderStatus.DRAFT, "Order must be DRAFT to place");
    ensure(!lines.isEmpty(), "Order must have at least 1 line");
    status = OrderStatus.PLACED;
  }

  public void cancel(String reason) {
    Objects.requireNonNull(reason);
    if (status == OrderStatus.CANCELLED) return;
    ensure(status != OrderStatus.PLACED, "For now: can't cancel after PLACED in this POC phase");
    status = OrderStatus.CANCELLED;
  }

  public static Order rehydrate(
      OrderId id,
      CustomerId customerId,
      Address shipTo,
      OrderStatus status,
      java.util.List<LineSnapshot> lines
  ) {
    Order o = new Order(id, customerId, shipTo);

    // build using existing invariant-safe methods
    for (var l : lines) {
      o.addLine(l.productId(), l.qty(), l.unitPrice());
    }

    // rebuild reachable states through transitions where possible
    switch (status) {
      case DRAFT -> {
        // no-op
      }
      case CREATED -> o.status = OrderStatus.CREATED;
      case PLACED -> o.place();
      case INVENTORY_RESERVED -> {
        o.place();
        o.markInventoryReserved();
      }
      case PAYMENT_AUTHORIZED -> {
        o.place();
        o.markInventoryReserved();
        o.markPaymentAuthorized();
      }
      case CONFIRMED -> {
        o.place();
        o.markInventoryReserved();
        o.markPaymentAuthorized();
        o.markCompleted();
      }
      case CANCELLED -> o.cancel("rehydrated");
      case FAILED -> o.status = OrderStatus.FAILED;
    }

    return o;
  }

  public void markInventoryReserved(){
    ensure(status == OrderStatus.PLACED, "Order must be PLACED to reserve inventory");
    this.status = OrderStatus.INVENTORY_RESERVED;
  }

  public void markPaymentAuthorized(){
    ensure(status == OrderStatus.INVENTORY_RESERVED, "Order must have inventory reserved to authorize payment");
    this.status = OrderStatus.PAYMENT_AUTHORIZED;
  }

  public void markCompleted(){
    ensure(status == OrderStatus.PAYMENT_AUTHORIZED, "Order must have payment authorized to complete");
    this.status = OrderStatus.CONFIRMED;
  }

  public void markFailed(){
    if (status == OrderStatus.FAILED) return;
    ensure(status != OrderStatus.CONFIRMED, "Can't fail a completed order");
    this.status = OrderStatus.FAILED;
  }

  private static void ensure(boolean ok, String msg) {
    if (!ok) throw new IllegalStateException(msg);
  }

  // Snapshot record (safe to expose)
  public record LineSnapshot(ProductId productId, int qty, Money unitPrice) {}
}
