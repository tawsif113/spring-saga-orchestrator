package com.example.saga.order.infra.amqp;

import com.example.saga.order.infra.mongo.OrderEventDocument;
import com.example.saga.order.infra.mongo.SpringDataOrderEventRepo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderEventOutboxPublisher {

  private final SpringDataOrderEventRepo eventRepo;
  private final RabbitTemplate rabbitTemplate;
  private final String exchangeName;

  public OrderEventOutboxPublisher(
      SpringDataOrderEventRepo eventRepo,
      RabbitTemplate rabbitTemplate,
      @Value("${saga.events.exchange:saga.events}") String exchangeName
  ) {
    this.eventRepo = eventRepo;
    this.rabbitTemplate = rabbitTemplate;
    this.exchangeName = exchangeName;
  }

  @Scheduled(fixedDelayString = "${saga.events.publisher-interval-ms:1000}")
  public void publishUnpublishedEvents() {
    List<OrderEventDocument> pending = eventRepo.findTop100ByPublishedAtIsNullOrderByOccurredAtAscVersionAsc();
    for (OrderEventDocument event : pending) {
      rabbitTemplate.convertAndSend(exchangeName, event.eventType, toMessage(event));
      event.publishedAt = Instant.now();
      eventRepo.save(event);
    }
  }

  private static Map<String, Object> toMessage(OrderEventDocument event) {
    Map<String, Object> message = new LinkedHashMap<>();
    message.put("eventId", event.id);
    message.put("eventType", event.eventType);
    message.put("occurredAt", event.occurredAt);
    message.put("aggregateId", event.aggregateId);
    message.put("sagaId", event.sagaId);
    message.put("version", event.version);
    message.put("payload", event.payload);
    return message;
  }
}
