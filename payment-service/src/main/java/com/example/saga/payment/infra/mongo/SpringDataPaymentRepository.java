package com.example.saga.payment.infra.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SpringDataPaymentRepository extends MongoRepository<PaymentDocument, String> {
  Optional<PaymentDocument> findByOrderId(String orderId);
}
