package com.example.saga.payment.domain;

import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.Ids.PaymentId;

import java.util.Optional;

public interface PaymentRepository {
  void save(Payment payment);
  Optional<Payment> findById(PaymentId paymentId);
  Optional<Payment> findByOrderId(OrderId orderId);
}
