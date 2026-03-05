package com.example.saga.inventory.infra.amqp;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryMessagingConfig {

  @Bean
  TopicExchange sagaEventsExchange(@Value("${saga.events.exchange:saga.events}") String exchangeName) {
    return new TopicExchange(exchangeName, true, false);
  }

  @Bean
  Queue orderPlacedQueue(@Value("${saga.events.order-placed-queue:inventory.order.placed}") String queueName) {
    return new Queue(queueName, true);
  }

  @Bean
  Binding orderPlacedBinding(Queue orderPlacedQueue, TopicExchange sagaEventsExchange) {
    return BindingBuilder.bind(orderPlacedQueue).to(sagaEventsExchange).with("order.placed");
  }


  @Bean
  Queue orderFailedQueue(@Value("${saga.events.order-failed-queue:inventory.order.failed}") String queueName) {
    return new Queue(queueName, true);
  }

  @Bean
  Binding orderFailedBinding(Queue orderFailedQueue, TopicExchange sagaEventsExchange) {
    return BindingBuilder.bind(orderFailedQueue).to(sagaEventsExchange).with("order.failed");
  }

  @Bean
  Queue orderConfirmedQueue(@Value("${saga.events.order-confirmed-queue:inventory.order.confirmed}") String queueName) {
    return new Queue(queueName, true);
  }

  @Bean
  Binding orderConfirmedBinding(Queue orderConfirmedQueue, TopicExchange sagaEventsExchange) {
    return BindingBuilder.bind(orderConfirmedQueue).to(sagaEventsExchange).with("order.confirmed");
  }

  @Bean
  MessageConverter jacksonMessageConverter() {
    return new JacksonJsonMessageConverter();
  }
}
