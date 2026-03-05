package com.example.saga.payment.app;

import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.Ids.PaymentId;
import com.example.saga.common.Money;
import com.example.saga.payment.domain.Payment;
import com.example.saga.payment.domain.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentCommandService {

  private final PaymentRepository repo;

  public PaymentCommandService(PaymentRepository repo) {
    this.repo = repo;
  }

  public Payment create(OrderId orderId, Money amount) {
    repo.findByOrderId(orderId).ifPresent(existing -> {
      throw new IllegalStateException("Payment already exists for orderId");
    });

    Payment payment = new Payment(new PaymentId(UUID.randomUUID()), orderId, amount);
    repo.save(payment);
    return payment;
  }

  public Payment authorize(PaymentId paymentId, String authCode) {
    Payment payment = get(paymentId);
    payment.authorize(authCode);
    repo.save(payment);
    return payment;
  }

  public Payment reject(PaymentId paymentId, String reason) {
    Payment payment = get(paymentId);
    payment.reject(reason);
    repo.save(payment);
    return payment;
  }

  public Payment get(PaymentId paymentId) {
    return repo.findById(paymentId)
        .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
  }

  public Payment getByOrderId(OrderId orderId) {
    return repo.findByOrderId(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId"));
  }

  public Optional<Payment> findByOrderIdIfExists(OrderId orderId) {
    return repo.findByOrderId(orderId);
  }
}
