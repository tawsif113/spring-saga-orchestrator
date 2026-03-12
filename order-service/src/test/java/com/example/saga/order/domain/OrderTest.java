package com.example.saga.order.domain;

import com.example.saga.common.Address;
import com.example.saga.common.Ids.CustomerId;
import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.Ids.ProductId;
import com.example.saga.common.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

  @Test
  void totalUsesCurrencyFromOrderLines() {
    Order order = new Order(
        new OrderId(UUID.randomUUID()),
        new CustomerId(UUID.randomUUID()),
        new Address("Dhaka", "Main Street", "42")
    );

    order.addLine(new ProductId(UUID.randomUUID()), 2, new Money(new BigDecimal("12.50"), "USD"));

    Money total = order.total();
    assertEquals("USD", total.currency());
    assertEquals(new BigDecimal("25.00"), total.amount());
  }

  @Test
  void addingSameProductWithDifferentUnitPriceThrows() {
    ProductId productId = new ProductId(UUID.randomUUID());
    Order order = new Order(
        new OrderId(UUID.randomUUID()),
        new CustomerId(UUID.randomUUID()),
        new Address("Dhaka", "Main Street", "42")
    );

    order.addLine(productId, 1, new Money(new BigDecimal("10.00"), "USD"));

    assertThrows(
        IllegalArgumentException.class,
        () -> order.addLine(productId, 1, new Money(new BigDecimal("11.00"), "USD"))
    );
  }
}
