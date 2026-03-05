package com.example.saga.payment.infra.mongo;

import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.Ids.PaymentId;
import com.example.saga.payment.domain.Payment;
import com.example.saga.payment.domain.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MongoPaymentRepository implements PaymentRepository {

  private final SpringDataPaymentRepository repo;

  public MongoPaymentRepository(SpringDataPaymentRepository repo) {
    this.repo = repo;
  }

  @Override
  public void save(Payment payment) {
    repo.save(PaymentMapper.toDocument(payment));
  }

  @Override
  public Optional<Payment> findById(PaymentId paymentId) {
    return repo.findById(paymentId.value().toString())
        .map(PaymentMapper::toDomain);
  }

  @Override
  public Optional<Payment> findByOrderId(OrderId orderId) {
    return repo.findByOrderId(orderId.value().toString())
        .map(PaymentMapper::toDomain);
  }
}
