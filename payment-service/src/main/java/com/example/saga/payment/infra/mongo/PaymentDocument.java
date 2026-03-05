package com.example.saga.payment.infra.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document(collection = "payments")
public class PaymentDocument {
  @Id
  @Indexed
  public String id;

  public String orderId;
  public BigDecimal amount;
  public String currency;

  public String status;
  public String authCode;
  public String rejectionReason;
}
