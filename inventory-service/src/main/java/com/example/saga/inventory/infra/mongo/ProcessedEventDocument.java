package com.example.saga.inventory.infra.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "processed_events")
public class ProcessedEventDocument {
  @Id
  public String eventId;

  public String eventType;
  public Instant processedAt;
}
