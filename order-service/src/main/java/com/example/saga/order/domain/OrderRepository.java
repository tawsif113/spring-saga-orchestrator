package com.example.saga.order.domain;

import com.example.saga.common.Ids.OrderId;

import java.util.Optional;

public interface OrderRepository {
  void save(Order order);
  Optional<Order> findById(OrderId id);
}