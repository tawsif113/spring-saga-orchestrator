package com.example.saga.payment.infra.amqp;

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
public class PaymentMessagingConfig {

  @Bean
  TopicExchange sagaEventsExchange(@Value("${saga.events.exchange:saga.events}") String exchangeName) {
    return new TopicExchange(exchangeName, true, false);
  }

  @Bean
  Queue orderInventoryReservedQueue(
      @Value("${saga.events.order-inventory-reserved-queue:payment.order.inventory-reserved}") String queueName
  ) {
    return new Queue(queueName, true);
  }

  @Bean
  Binding orderInventoryReservedBinding(Queue orderInventoryReservedQueue, TopicExchange sagaEventsExchange) {
    return BindingBuilder.bind(orderInventoryReservedQueue).to(sagaEventsExchange).with("order.inventory-reserved");
  }

  @Bean
  MessageConverter jacksonMessageConverter() {
    return new JacksonJsonMessageConverter();
  }
}
