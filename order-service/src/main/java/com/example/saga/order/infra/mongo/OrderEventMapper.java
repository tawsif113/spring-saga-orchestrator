package com.example.saga.order.infra.mongo;

import com.example.saga.common.events.DomainEvent;

import java.util.Map;
import java.util.UUID;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

final class OrderEventMapper {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
  };

  static OrderEventDocument toDoc(DomainEvent event, ObjectMapper objectMapper) {
    var d = new OrderEventDocument();
    d.id = event.eventId().toString();
    d.eventType = event.eventType();
    d.occurredAt = event.occurredAt();
    d.aggregateId = event.aggregateId().toString();
    d.sagaId = event.sagaId() == null ? null : event.sagaId().toString();
    d.version = event.version();
    d.payload = objectMapper.convertValue(event.payload(), MAP_TYPE);
    return d;
  }

  static DomainEvent toDomain(OrderEventDocument d, ObjectMapper objectMapper) {
    Map<String, Object> payload = d.payload == null ? Map.of() : objectMapper.convertValue(d.payload, MAP_TYPE);
    return new DomainEvent(
        UUID.fromString(d.id),
        d.eventType,
        d.occurredAt,
        UUID.fromString(d.aggregateId),
        d.sagaId == null ? null : UUID.fromString(d.sagaId),
        d.version,
        payload
    );
  }

  private OrderEventMapper() {
  }
}
