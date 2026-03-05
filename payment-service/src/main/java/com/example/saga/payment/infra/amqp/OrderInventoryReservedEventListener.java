package com.example.saga.payment.infra.amqp;

import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.Money;
import com.example.saga.common.events.DomainEvent;
import com.example.saga.common.events.OrderEventTypes;
import com.example.saga.common.events.PaymentAuthorizedPayload;
import com.example.saga.common.events.PaymentEventTypes;
import com.example.saga.common.events.PaymentRejectedPayload;
import com.example.saga.payment.app.PaymentCommandService;
import com.example.saga.payment.domain.Payment;
import com.example.saga.payment.infra.mongo.ProcessedEventDocument;
import com.example.saga.payment.infra.mongo.SpringDataProcessedEventRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderInventoryReservedEventListener {

  private final PaymentCommandService paymentService;
  private final PaymentEventPublisher eventPublisher;
  private final SpringDataProcessedEventRepository processedEventRepo;
  private final BigDecimal autoApproveLimit;

  public OrderInventoryReservedEventListener(
      PaymentCommandService paymentService,
      PaymentEventPublisher eventPublisher,
      SpringDataProcessedEventRepository processedEventRepo,
      @Value("${saga.payment.auto-approve-limit:10000.00}") BigDecimal autoApproveLimit
  ) {
    this.paymentService = paymentService;
    this.eventPublisher = eventPublisher;
    this.processedEventRepo = processedEventRepo;
    this.autoApproveLimit = autoApproveLimit;
  }

  @RabbitListener(queues = "${saga.events.order-inventory-reserved-queue:payment.order.inventory-reserved}")
  public void onOrderInventoryReserved(Map<String, Object> event) {
    String eventId = requiredString(event, "eventId");
    if (processedEventRepo.existsById(eventId)) {
      return;
    }

    String eventType = requiredString(event, "eventType");
    if (!OrderEventTypes.ORDER_INVENTORY_RESERVED.equals(eventType)) {
      return;
    }

    UUID sagaId = optionalUuid(event, "sagaId");
    Map<String, Object> payload = requiredMap(event, "payload");
    UUID orderId = UUID.fromString(requiredString(payload, "orderId"));

    Payment payment = paymentService.findByOrderIdIfExists(new OrderId(orderId))
        .orElseGet(() -> paymentService.create(
            new OrderId(orderId),
            new Money(
                requiredDecimal(payload, "totalAmount"),
                requiredString(payload, "currency")
            )
        ));

    DomainEvent outcome = switch (payment.status()) {
      case AUTHORIZED -> paymentAuthorizedEvent(payment, sagaId);
      case REJECTED -> paymentRejectedEvent(payment, sagaId, safeReason(payment.rejectionReason()));
      case CREATED -> processFreshPayment(payment, sagaId);
    };

    eventPublisher.publish(outcome);
    markProcessed(eventId, eventType);
  }

  private DomainEvent processFreshPayment(Payment payment, UUID sagaId) {
    if (payment.amount().amount().compareTo(autoApproveLimit) <= 0) {
      Payment authorized = paymentService.authorize(payment.id(), generateAuthCode());
      return paymentAuthorizedEvent(authorized, sagaId);
    }
    String reason = "Amount exceeds auto-approve limit";
    Payment rejected = paymentService.reject(payment.id(), reason);
    return paymentRejectedEvent(rejected, sagaId, reason);
  }

  private DomainEvent paymentAuthorizedEvent(Payment payment, UUID sagaId) {
    return new DomainEvent(
        UUID.randomUUID(),
        PaymentEventTypes.PAYMENT_AUTHORIZED,
        Instant.now(),
        payment.id().value(),
        sagaId,
        1,
        new PaymentAuthorizedPayload(
            payment.orderId().value(),
            payment.id().value(),
            payment.authCode()
        )
    );
  }

  private DomainEvent paymentRejectedEvent(Payment payment, UUID sagaId, String reason) {
    return new DomainEvent(
        UUID.randomUUID(),
        PaymentEventTypes.PAYMENT_REJECTED,
        Instant.now(),
        payment.id().value(),
        sagaId,
        1,
        new PaymentRejectedPayload(
            payment.orderId().value(),
            payment.id().value(),
            reason
        )
    );
  }

  private void markProcessed(String eventId, String eventType) {
    ProcessedEventDocument processed = new ProcessedEventDocument();
    processed.eventId = eventId;
    processed.eventType = eventType;
    processed.processedAt = Instant.now();
    processedEventRepo.save(processed);
  }

  private static String generateAuthCode() {
    return "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private static String safeReason(String reason) {
    if (reason == null || reason.isBlank()) {
      return "Payment rejected";
    }
    return reason;
  }

  private static String requiredString(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (!(value instanceof String s) || s.isBlank()) {
      throw new IllegalArgumentException(key + " is required");
    }
    return s;
  }

  private static BigDecimal requiredDecimal(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (value instanceof Number n) {
      return new BigDecimal(n.toString());
    }
    if (value instanceof String s && !s.isBlank()) {
      return new BigDecimal(s);
    }
    throw new IllegalArgumentException(key + " must be a number");
  }

  private static Map<String, Object> requiredMap(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (value instanceof Map<?, ?> mapRaw) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) mapRaw;
      return map;
    }
    throw new IllegalArgumentException(key + " is required");
  }

  private static UUID optionalUuid(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (value == null) {
      return null;
    }
    if (value instanceof String s && !s.isBlank()) {
      return UUID.fromString(s);
    }
    throw new IllegalArgumentException(key + " must be a UUID string");
  }
}
