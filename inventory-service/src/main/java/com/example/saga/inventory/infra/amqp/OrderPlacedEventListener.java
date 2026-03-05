package com.example.saga.inventory.infra.amqp;

import com.example.saga.common.Ids.ProductId;
import com.example.saga.common.events.DomainEvent;
import com.example.saga.common.events.InventoryEventTypes;
import com.example.saga.common.events.InventoryRejectedPayload;
import com.example.saga.common.events.InventoryReservedPayload;
import com.example.saga.common.events.OrderEventTypes;
import com.example.saga.inventory.app.StockCommandService;
import com.example.saga.inventory.infra.mongo.ProcessedEventDocument;
import com.example.saga.inventory.infra.mongo.SpringDataProcessedEventRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderPlacedEventListener {

  private final StockCommandService stockService;
  private final SpringDataProcessedEventRepository processedEventRepo;
  private final InventoryEventPublisher eventPublisher;

  public OrderPlacedEventListener(
      StockCommandService stockService,
      SpringDataProcessedEventRepository processedEventRepo,
      InventoryEventPublisher eventPublisher
  ) {
    this.stockService = stockService;
    this.processedEventRepo = processedEventRepo;
    this.eventPublisher = eventPublisher;
  }

  @RabbitListener(queues = "${saga.events.order-placed-queue:inventory.order.placed}")
  public void onOrderPlaced(Map<String, Object> event) {
    String eventId = requiredString(event, "eventId");
    if (processedEventRepo.existsById(eventId)) {
      return;
    }

    String eventType = requiredString(event, "eventType");
    if (!OrderEventTypes.ORDER_PLACED.equals(eventType)) {
      return;
    }

    UUID sagaId = optionalUuid(event, "sagaId");
    Map<String, Object> payload = requiredMap(event, "payload");
    UUID orderId = UUID.fromString(requiredString(payload, "orderId"));
    List<?> lines = requiredList(payload, "lines");

    try {
      for (Object lineObj : lines) {
        if (!(lineObj instanceof Map<?, ?> lineMapRaw)) {
          throw new IllegalArgumentException("line payload must be an object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> lineMap = (Map<String, Object>) lineMapRaw;
        UUID productId = UUID.fromString(requiredString(lineMap, "productId"));
        int qty = requiredInt(lineMap, "qty");
        stockService.reserve(new ProductId(productId), qty);
      }
    } catch (RuntimeException ex) {
      eventPublisher.publish(new DomainEvent(
          UUID.randomUUID(),
          InventoryEventTypes.INVENTORY_REJECTED,
          Instant.now(),
          orderId,
          sagaId,
          1,
          new InventoryRejectedPayload(orderId, safeReason(ex))
      ));
      markProcessed(eventId, eventType);
      return;
    }

    eventPublisher.publish(new DomainEvent(
        UUID.randomUUID(),
        InventoryEventTypes.INVENTORY_RESERVED,
        Instant.now(),
        orderId,
        sagaId,
        1,
        new InventoryReservedPayload(orderId)
    ));
    markProcessed(eventId, eventType);
  }

  private static String requiredString(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (!(value instanceof String s) || s.isBlank()) {
      throw new IllegalArgumentException(key + " is required");
    }
    return s;
  }

  private static int requiredInt(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (value instanceof Number n) {
      return n.intValue();
    }
    throw new IllegalArgumentException(key + " must be a number");
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

  private static List<?> requiredList(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (value instanceof List<?> list) {
      return list;
    }
    throw new IllegalArgumentException(key + " is required");
  }

  private static UUID optionalUuid(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (value == null) {
      return null;
    }
    if (value instanceof String s && !s.isBlank()) {
      return UUID.fromString(s);
    }
    throw new IllegalArgumentException(key + " must be a UUID string");
  }

  private static String safeReason(Exception ex) {
    String message = ex.getMessage();
    return (message == null || message.isBlank()) ? "Inventory reservation failed" : message;
  }

  private void markProcessed(String eventId, String eventType) {
    ProcessedEventDocument processed = new ProcessedEventDocument();
    processed.eventId = eventId;
    processed.eventType = eventType;
    processed.processedAt = Instant.now();
    processedEventRepo.save(processed);
  }
}
