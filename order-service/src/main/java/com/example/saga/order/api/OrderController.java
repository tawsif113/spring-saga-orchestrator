package com.example.saga.order.api;

import com.example.saga.common.Address;
import com.example.saga.common.Money;
import com.example.saga.common.Ids.CustomerId;
import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.Ids.ProductId;
import com.example.saga.common.events.DomainEvent;
import com.example.saga.order.app.OrderCommandService;
import com.example.saga.order.domain.Order;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderCommandService service;

  public OrderController(OrderCommandService service) {
    this.service = service;
  }

  public record CreateOrderRequest(UUID customerId, AddressDto shipTo) {}
  public record AddressDto(String city, String street, String houseNo) {}
  public record AddLineRequest(UUID productId, int qty, BigDecimal unitAmount, String currency) {}

  public record OrderResponse(
      UUID orderId,
      UUID customerId,
      String status,
      AddressDto shipTo,
      List<LineResponse> lines,
      BigDecimal totalAmount,
      String currency
  ) {}
  public record LineResponse(UUID productId, int qty, BigDecimal unitAmount, String currency) {}
  public record OrderEventResponse(
      UUID eventId,
      String eventType,
      Instant occurredAt,
      UUID aggregateId,
      UUID sagaId,
      int version,
      Object payload
  ) {}

  @PostMapping
  public OrderResponse create(@RequestBody CreateOrderRequest req) {
    Order o = service.create(
        new CustomerId(req.customerId()),
        new Address(req.shipTo.city(), req.shipTo.street(), req.shipTo.houseNo())
    );
    return toResponse(o);
  }

  @PostMapping("/{id}/lines")
  public OrderResponse addLine(@PathVariable UUID id, @RequestBody AddLineRequest req) {
    Order o = service.addLine(
        new OrderId(id),
        new ProductId(req.productId()),
        req.qty(),
        new Money(req.unitAmount(), req.currency())
    );
    return toResponse(o);
  }

  @PostMapping("/{id}/place")
  public OrderResponse place(@PathVariable UUID id) {
    Order o = service.place(new OrderId(id));
    return toResponse(o);
  }

  @GetMapping("/{id}")
  public OrderResponse get(@PathVariable UUID id) {
    return toResponse(service.get(new OrderId(id)));
  }

  @PostMapping("/{orderId}/inventory-reserved")
  public OrderResponse inventoryReserved(@PathVariable UUID orderId) {
    return toResponse(service.markInventoryReserved(new OrderId(orderId)));
  }

  @PostMapping("/{orderId}/payment-authorized")
  public OrderResponse paymentAuthorized(@PathVariable UUID orderId) {
    return toResponse(service.markPaymentAuthorized(new OrderId(orderId)));
  }

  @PostMapping("/{orderId}/confirm")
  public OrderResponse confirm(@PathVariable UUID orderId) {
    return toResponse(service.confirm(new OrderId(orderId)));
  }

  @PostMapping("/{orderId}/fail")
  public OrderResponse fail(@PathVariable UUID orderId) {
    return toResponse(service.fail(new OrderId(orderId)));
  }

  @GetMapping("/{orderId}/events")
  public List<OrderEventResponse> events(@PathVariable UUID orderId) {
    return service.getEvents(new OrderId(orderId)).stream()
        .map(OrderController::toEventResponse)
        .toList();
  }

  private static OrderResponse toResponse(Order o) {
    var lines = o.lineSnapshots().stream()
        .map(l -> new LineResponse(l.productId().value(), l.qty(), l.unitPrice().amount(), l.unitPrice().currency()))
        .toList();

    var total = o.total();
    var shipTo = o.shipTo();
    return new OrderResponse(
        o.id().value(),
        o.customerId().value(),
        o.status().name(),
        new AddressDto(shipTo.city(), shipTo.street(), shipTo.houseNo()),
        lines,
        total.amount(),
        total.currency()
    );
  }

  private static OrderEventResponse toEventResponse(DomainEvent event) {
    return new OrderEventResponse(
        event.eventId(),
        event.eventType(),
        event.occurredAt(),
        event.aggregateId(),
        event.sagaId(),
        event.version(),
        event.payload()
    );
  }
}
