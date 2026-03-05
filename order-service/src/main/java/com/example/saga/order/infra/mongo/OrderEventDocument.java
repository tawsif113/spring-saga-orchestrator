package com.example.saga.order.infra.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "order_events")
public class OrderEventDocument {
  @Id
  public String id;

  public String eventType;
  public Instant occurredAt;
  public String aggregateId;
  public String sagaId;
  public int version;
  public Map<String, Object> payload;
  public Instant publishedAt;
}
