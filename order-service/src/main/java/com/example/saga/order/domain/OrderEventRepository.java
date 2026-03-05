package com.example.saga.order.domain;

import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.events.DomainEvent;

import java.util.List;

public interface OrderEventRepository {
  void append(DomainEvent event);
  List<DomainEvent> findByAggregateId(OrderId orderId);
  int nextVersion(OrderId orderId);
}
