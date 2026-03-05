package com.example.saga.payment.infra.mongo;

import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.Ids.PaymentId;
import com.example.saga.common.Money;
import com.example.saga.payment.domain.Payment;
import com.example.saga.payment.domain.PaymentStatus;

import java.util.UUID;

final class PaymentMapper {

  static PaymentDocument toDocument(Payment payment) {
    PaymentDocument d = new PaymentDocument();
    d.id = payment.id().value().toString();
    d.orderId = payment.orderId().value().toString();
    d.amount = payment.amount().amount();
    d.currency = payment.amount().currency();
    d.status = payment.status().name();
    d.authCode = payment.authCode();
    d.rejectionReason = payment.rejectionReason();
    return d;
  }

  static Payment toDomain(PaymentDocument d) {
    return Payment.rehydrate(
        new PaymentId(UUID.fromString(d.id)),
        new OrderId(UUID.fromString(d.orderId)),
        new Money(d.amount, d.currency),
        PaymentStatus.valueOf(d.status),
        d.authCode,
        d.rejectionReason
    );
  }

  private PaymentMapper() {
  }
}
