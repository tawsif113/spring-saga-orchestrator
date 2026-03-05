package com.example.saga.common;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {
  public Money {
    Objects.requireNonNull(amount);
    Objects.requireNonNull(currency);
    if (amount.signum() < 0) throw new IllegalArgumentException("amount must be >= 0");
  }

  public Money add(Money other) {
    if (!currency.equals(other.currency)) throw new IllegalArgumentException("currency mismatch");
    return new Money(amount.add(other.amount), currency);
  }

  public Money multiply(int qty) {
    if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");
    return new Money(amount.multiply(BigDecimal.valueOf(qty)), currency);
  }
}
