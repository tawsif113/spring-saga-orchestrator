package com.example.saga.order.app;

import com.example.saga.common.Address;
import com.example.saga.common.Money;
import com.example.saga.common.Ids.CustomerId;
import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.Ids.ProductId;
import com.example.saga.common.events.DomainEvent;
import com.example.saga.common.events.OrderConfirmedPayload;
import com.example.saga.common.events.OrderEventTypes;
import com.example.saga.common.events.OrderFailedPayload;
import com.example.saga.common.events.OrderInventoryReservedPayload;
import com.example.saga.common.events.OrderPaymentAuthorizedPayload;
import com.example.saga.common.events.OrderPlacedPayload;
import com.example.saga.order.domain.Order;
import com.example.saga.order.domain.OrderEventRepository;
import com.example.saga.order.domain.OrderRepository;
import com.example.saga.order.domain.OrderStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderCommandService {
  private final OrderRepository repo;
  private final OrderEventRepository eventRepo;

  public OrderCommandService(OrderRepository repo, OrderEventRepository eventRepo) {
    this.repo = repo;
    this.eventRepo = eventRepo;
  }

  public Order create(CustomerId customerId, Address shipTo) {
    Order order = new Order(new OrderId(UUID.randomUUID()), customerId, shipTo);
    repo.save(order);
    return order;
  }

  public Order addLine(OrderId orderId, ProductId productId, int qty, Money unitPrice) {
    Order order = repo.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
    order.addLine(productId, qty, unitPrice);
    repo.save(order);
    return order;
  }

  public Order place(OrderId orderId) {
    Order order = repo.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
    order.place();
    repo.save(order);
    appendEvent(
        orderId,
        OrderEventTypes.ORDER_PLACED,
        new OrderPlacedPayload(
            order.id().value(),
            order.customerId().value(),
            order.total().amount(),
            order.total().currency(),
            order.lineSnapshots().stream()
                .map(l -> new OrderPlacedPayload.OrderLinePayload(l.productId().value(), l.qty()))
                .toList()
        )
    );
    return order;
  }

  public Order get(OrderId orderId) {
    return repo.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
  }

  public Order markInventoryReserved(OrderId orderId) {
    Order order = get(orderId);
    order.markInventoryReserved();
    repo.save(order);
    appendEvent(
        orderId,
        OrderEventTypes.ORDER_INVENTORY_RESERVED,
        new OrderInventoryReservedPayload(
            order.id().value(),
            order.total().amount(),
            order.total().currency()
        )
    );
    return order;
  }

  public Order markPaymentAuthorized(OrderId orderId) {
    Order order = get(orderId);
    order.markPaymentAuthorized();
    repo.save(order);
    appendEvent(
        orderId,
        OrderEventTypes.ORDER_PAYMENT_AUTHORIZED,
        new OrderPaymentAuthorizedPayload(order.id().value())
    );
    return order;
  }

  public Order confirm(OrderId orderId) {
    Order order = get(orderId);
    order.markCompleted();
    repo.save(order);
    appendEvent(
        orderId,
        OrderEventTypes.ORDER_CONFIRMED,
        new OrderConfirmedPayload(
            order.id().value(),
            order.lineSnapshots().stream()
                .map(l -> new OrderConfirmedPayload.OrderLinePayload(l.productId().value(), l.qty()))
                .toList()
        )
    );
    return order;
  }

  public Order fail(OrderId orderId) {
    return fail(orderId, "MANUAL_FAILURE");
  }

  public Order fail(OrderId orderId, String reason) {
    Order order = get(orderId);
    OrderStatus before = order.status();
    order.markFailed();
    repo.save(order);
    if (before != OrderStatus.FAILED) {
      appendEvent(
          orderId,
          OrderEventTypes.ORDER_FAILED,
          new OrderFailedPayload(
              order.id().value(),
              reason,
              order.lineSnapshots().stream()
                  .map(l -> new OrderFailedPayload.OrderLinePayload(l.productId().value(), l.qty()))
                  .toList()
          )
      );
    }
    return order;
  }

  public Order handleInventoryReservedEvent(OrderId orderId) {
    Order order = get(orderId);
    if (order.status() == OrderStatus.INVENTORY_RESERVED
        || order.status() == OrderStatus.PAYMENT_AUTHORIZED
        || order.status() == OrderStatus.CONFIRMED
        || order.status() == OrderStatus.FAILED
        || order.status() == OrderStatus.CANCELLED) {
      return order;
    }
    return markInventoryReserved(orderId);
  }

  public Order handleInventoryRejectedEvent(OrderId orderId, String reason) {
    Order order = get(orderId);
    if (order.status() == OrderStatus.FAILED
        || order.status() == OrderStatus.CANCELLED
        || order.status() == OrderStatus.CONFIRMED) {
      return order;
    }
    return fail(orderId, reason);
  }

  public Order handlePaymentAuthorizedEvent(OrderId orderId) {
    Order order = get(orderId);
    if (order.status() == OrderStatus.PAYMENT_AUTHORIZED
        || order.status() == OrderStatus.CONFIRMED
        || order.status() == OrderStatus.FAILED
        || order.status() == OrderStatus.CANCELLED) {
      return order;
    }
    if (order.status() != OrderStatus.INVENTORY_RESERVED) {
      return order;
    }
    markPaymentAuthorized(orderId);
    return confirm(orderId);
  }

  public Order handlePaymentRejectedEvent(OrderId orderId, String reason) {
    Order order = get(orderId);
    if (order.status() == OrderStatus.FAILED
        || order.status() == OrderStatus.CANCELLED
        || order.status() == OrderStatus.CONFIRMED) {
      return order;
    }
    return fail(orderId, reason);
  }

  public List<DomainEvent> getEvents(OrderId orderId) {
    get(orderId);
    return eventRepo.findByAggregateId(orderId);
  }

  private void appendEvent(OrderId orderId, String eventType, Object payload) {
    eventRepo.append(new DomainEvent(
        UUID.randomUUID(),
        eventType,
        Instant.now(),
        orderId.value(),
        null,
        eventRepo.nextVersion(orderId),
        payload
    ));
  }
}
