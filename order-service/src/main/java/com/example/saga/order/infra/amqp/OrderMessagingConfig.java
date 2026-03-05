package com.example.saga.order.infra.amqp;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderMessagingConfig {

  @Bean
  TopicExchange sagaEventsExchange(@Value("${saga.events.exchange:saga.events}") String exchangeName) {
    return new TopicExchange(exchangeName, true, false);
  }

  @Bean
  Queue sagaAuditQueue(@Value("${saga.events.audit-queue:saga.events.audit}") String queueName) {
    return new Queue(queueName, true);
  }

  @Bean
  Binding sagaAuditBinding(Queue sagaAuditQueue, TopicExchange sagaEventsExchange) {
    return BindingBuilder.bind(sagaAuditQueue).to(sagaEventsExchange).with("#");
  }

  @Bean
  Queue inventoryReservedQueue(
      @Value("${saga.events.inventory-reserved-queue:order.inventory.reserved}") String queueName
  ) {
    return new Queue(queueName, true);
  }

  @Bean
  Binding inventoryReservedBinding(Queue inventoryReservedQueue, TopicExchange sagaEventsExchange) {
    return BindingBuilder.bind(inventoryReservedQueue).to(sagaEventsExchange).with("inventory.reserved");
  }

  @Bean
  Queue inventoryRejectedQueue(
      @Value("${saga.events.inventory-rejected-queue:order.inventory.rejected}") String queueName
  ) {
    return new Queue(queueName, true);
  }

  @Bean
  Binding inventoryRejectedBinding(Queue inventoryRejectedQueue, TopicExchange sagaEventsExchange) {
    return BindingBuilder.bind(inventoryRejectedQueue).to(sagaEventsExchange).with("inventory.rejected");
  }

  @Bean
  Queue paymentAuthorizedQueue(
      @Value("${saga.events.payment-authorized-queue:order.payment.authorized}") String queueName
  ) {
    return new Queue(queueName, true);
  }

  @Bean
  Binding paymentAuthorizedBinding(Queue paymentAuthorizedQueue, TopicExchange sagaEventsExchange) {
    return BindingBuilder.bind(paymentAuthorizedQueue).to(sagaEventsExchange).with("payment.authorized");
  }

  @Bean
  Queue paymentRejectedQueue(
      @Value("${saga.events.payment-rejected-queue:order.payment.rejected}") String queueName
  ) {
    return new Queue(queueName, true);
  }

  @Bean
  Binding paymentRejectedBinding(Queue paymentRejectedQueue, TopicExchange sagaEventsExchange) {
    return BindingBuilder.bind(paymentRejectedQueue).to(sagaEventsExchange).with("payment.rejected");
  }

  @Bean
  MessageConverter jacksonMessageConverter() {
    return new JacksonJsonMessageConverter();
  }
}
