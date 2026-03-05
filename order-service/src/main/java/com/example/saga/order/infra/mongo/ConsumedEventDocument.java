package com.example.saga.order.infra.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "consumed_events")
public class ConsumedEventDocument {
  @Id
  public String eventId;

  public String eventType;
  public Instant consumedAt;
}
