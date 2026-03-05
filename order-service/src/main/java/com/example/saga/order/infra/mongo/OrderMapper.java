package com.example.saga.order.infra.mongo;

import com.example.saga.common.Address;
import com.example.saga.common.Money;
import com.example.saga.common.Ids.CustomerId;
import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.Ids.ProductId;
import com.example.saga.order.domain.Order;
import com.example.saga.order.domain.OrderStatus;

import java.util.UUID;

final class OrderMapper {

  static OrderDocument toDoc(Order o) {
    var d = new OrderDocument();
    d.id = o.id().value().toString();
    d.customerId = o.customerId().value().toString();
    d.status = o.status().name();

    var a = new OrderDocument.AddressDoc();
    a.city = o.shipTo().city();
    a.street = o.shipTo().street();
    a.houseNo = o.shipTo().houseNo();
    d.shipTo = a;

    for (var l : o.lineSnapshots()) {
      var ld = new OrderDocument.LineDoc();
      ld.productId = l.productId().value().toString();
      ld.qty = l.qty();
      ld.unitAmount = l.unitPrice().amount();
      ld.currency = l.unitPrice().currency();
      d.lines.add(ld);
    }
    return d;
  }

  static Order toDomain(OrderDocument d) {
    var orderId = new OrderId(UUID.fromString(d.id));
    var customerId = new CustomerId(UUID.fromString(d.customerId));
    var shipTo = new Address(d.shipTo.city, d.shipTo.street, d.shipTo.houseNo);
    var status = OrderStatus.valueOf(d.status);

    // Rehydrate cleanly: build as DRAFT, add lines, then set status inside Order
    var lines = d.lines.stream()
        .map(ld -> new Order.LineSnapshot(
            new ProductId(UUID.fromString(ld.productId)),
            ld.qty,
            new Money(ld.unitAmount, ld.currency)
        ))
        .toList();

    return Order.rehydrate(orderId, customerId, shipTo, status, lines);
  }

  private OrderMapper() {}
}