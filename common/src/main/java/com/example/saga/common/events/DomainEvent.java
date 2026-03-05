package com.example.saga.common.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DomainEvent(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    UUID aggregateId,
    UUID sagaId,
    int version,
    Object payload
) {
  public DomainEvent {
    Objects.requireNonNull(eventId, "eventId is required");
    Objects.requireNonNull(eventType, "eventType is required");
    Objects.requireNonNull(occurredAt, "occurredAt is required");
    Objects.requireNonNull(aggregateId, "aggregateId is required");
    Objects.requireNonNull(payload, "payload is required");
    if (eventType.isBlank()) {
      throw new IllegalArgumentException("eventType is required");
    }
    if (version <= 0) {
      throw new IllegalArgumentException("version must be > 0");
    }
  }
}
