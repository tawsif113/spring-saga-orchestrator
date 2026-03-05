package com.example.saga.order.infra.amqp;

import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.events.InventoryEventTypes;
import com.example.saga.order.app.OrderCommandService;
import com.example.saga.order.infra.mongo.ConsumedEventDocument;
import com.example.saga.order.infra.mongo.SpringDataConsumedEventRepo;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class InventoryOutcomeEventListener {

  private final OrderCommandService orderService;
  private final SpringDataConsumedEventRepo consumedEventRepo;

  public InventoryOutcomeEventListener(
      OrderCommandService orderService,
      SpringDataConsumedEventRepo consumedEventRepo
  ) {
    this.orderService = orderService;
    this.consumedEventRepo = consumedEventRepo;
  }

  @RabbitListener(queues = "${saga.events.inventory-reserved-queue:order.inventory.reserved}")
  public void onInventoryReserved(Map<String, Object> event) {
    String eventId = requiredString(event, "eventId");
    if (consumedEventRepo.existsById(eventId)) {
      return;
    }

    String eventType = requiredString(event, "eventType");
    if (!InventoryEventTypes.INVENTORY_RESERVED.equals(eventType)) {
      return;
    }

    Map<String, Object> payload = requiredMap(event, "payload");
    UUID orderId = UUID.fromString(requiredString(payload, "orderId"));

    orderService.handleInventoryReservedEvent(new OrderId(orderId));
    markConsumed(eventId, eventType);
  }

  @RabbitListener(queues = "${saga.events.inventory-rejected-queue:order.inventory.rejected}")
  public void onInventoryRejected(Map<String, Object> event) {
    String eventId = requiredString(event, "eventId");
    if (consumedEventRepo.existsById(eventId)) {
      return;
    }

    String eventType = requiredString(event, "eventType");
    if (!InventoryEventTypes.INVENTORY_REJECTED.equals(eventType)) {
      return;
    }

    Map<String, Object> payload = requiredMap(event, "payload");
    UUID orderId = UUID.fromString(requiredString(payload, "orderId"));
    String reason = requiredString(payload, "reason");

    orderService.handleInventoryRejectedEvent(new OrderId(orderId), reason);
    markConsumed(eventId, eventType);
  }

  private void markConsumed(String eventId, String eventType) {
    ConsumedEventDocument consumed = new ConsumedEventDocument();
    consumed.eventId = eventId;
    consumed.eventType = eventType;
    consumed.consumedAt = Instant.now();
    consumedEventRepo.save(consumed);
  }

  private static String requiredString(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (!(value instanceof String s) || s.isBlank()) {
      throw new IllegalArgumentException(key + " is required");
    }
    return s;
  }

  private static Map<String, Object> requiredMap(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (value instanceof Map<?, ?> mapRaw) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) mapRaw;
      return map;
    }
    throw new IllegalArgumentException(key + " is required");
  }
}
