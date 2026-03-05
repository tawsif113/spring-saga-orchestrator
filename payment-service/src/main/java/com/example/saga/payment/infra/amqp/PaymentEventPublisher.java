package com.example.saga.payment.infra.amqp;

import com.example.saga.common.events.DomainEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PaymentEventPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final String exchangeName;

  public PaymentEventPublisher(
      RabbitTemplate rabbitTemplate,
      @Value("${saga.events.exchange:saga.events}") String exchangeName
  ) {
    this.rabbitTemplate = rabbitTemplate;
    this.exchangeName = exchangeName;
  }

  public void publish(DomainEvent event) {
    rabbitTemplate.convertAndSend(exchangeName, event.eventType(), toMessage(event));
  }

  private static Map<String, Object> toMessage(DomainEvent event) {
    Map<String, Object> message = new LinkedHashMap<>();
    message.put("eventId", event.eventId().toString());
    message.put("eventType", event.eventType());
    message.put("occurredAt", event.occurredAt());
    message.put("aggregateId", event.aggregateId().toString());
    message.put("sagaId", event.sagaId() == null ? null : event.sagaId().toString());
    message.put("version", event.version());
    message.put("payload", event.payload());
    return message;
  }
}
