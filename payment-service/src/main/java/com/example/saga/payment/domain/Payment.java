package com.example.saga.payment.domain;

import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.Ids.PaymentId;
import com.example.saga.common.Money;

import java.util.Objects;

public class Payment { // Aggregate Root
  private final PaymentId id;
  private final OrderId orderId;
  private final Money amount;

  private PaymentStatus status;
  private String authCode;
  private String rejectionReason;

  public Payment(PaymentId id, OrderId orderId, Money amount) {
    this(id, orderId, amount, PaymentStatus.CREATED, null, null);
  }

  private Payment(
      PaymentId id,
      OrderId orderId,
      Money amount,
      PaymentStatus status,
      String authCode,
      String rejectionReason
  ) {
    this.id = Objects.requireNonNull(id);
    this.orderId = Objects.requireNonNull(orderId);
    this.amount = Objects.requireNonNull(amount);
    this.status = Objects.requireNonNull(status);
    this.authCode = authCode;
    this.rejectionReason = rejectionReason;

    if (amount.amount().signum() <= 0) {
      throw new IllegalArgumentException("amount must be > 0");
    }

    validateState();
  }

  public static Payment rehydrate(
      PaymentId id,
      OrderId orderId,
      Money amount,
      PaymentStatus status,
      String authCode,
      String rejectionReason
  ) {
    return new Payment(id, orderId, amount, status, authCode, rejectionReason);
  }

  public PaymentId id() {
    return id;
  }

  public OrderId orderId() {
    return orderId;
  }

  public Money amount() {
    return amount;
  }

  public PaymentStatus status() {
    return status;
  }

  public String authCode() {
    return authCode;
  }

  public String rejectionReason() {
    return rejectionReason;
  }

  public void authorize(String authCode) {
    if (status == PaymentStatus.AUTHORIZED) {
      throw new IllegalStateException("Payment already authorized");
    }
    if (status == PaymentStatus.REJECTED) {
      throw new IllegalStateException("Rejected payment cannot be authorized");
    }
    if (authCode == null || authCode.isBlank()) {
      throw new IllegalArgumentException("authCode is required");
    }

    this.status = PaymentStatus.AUTHORIZED;
    this.authCode = authCode;
    this.rejectionReason = null;

    validateState();
  }

  public void reject(String reason) {
    if (status == PaymentStatus.REJECTED) {
      throw new IllegalStateException("Payment already rejected");
    }
    if (status == PaymentStatus.AUTHORIZED) {
      throw new IllegalStateException("Authorized payment cannot be rejected");
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("reason is required");
    }

    this.status = PaymentStatus.REJECTED;
    this.rejectionReason = reason;
    this.authCode = null;

    validateState();
  }

  private void validateState() {
    if (status == PaymentStatus.AUTHORIZED && (authCode == null || authCode.isBlank())) {
      throw new IllegalStateException("Authorized payment must have authCode");
    }
    if (status == PaymentStatus.REJECTED && (rejectionReason == null || rejectionReason.isBlank())) {
      throw new IllegalStateException("Rejected payment must have rejectionReason");
    }
    if (status == PaymentStatus.CREATED && (authCode != null || rejectionReason != null)) {
      throw new IllegalStateException("Created payment cannot have authCode or rejectionReason");
    }
  }
}
