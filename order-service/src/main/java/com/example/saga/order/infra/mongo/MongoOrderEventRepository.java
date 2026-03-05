package com.example.saga.order.infra.mongo;

import com.example.saga.common.Ids.OrderId;
import com.example.saga.common.events.DomainEvent;
import com.example.saga.order.domain.OrderEventRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import tools.jackson.databind.ObjectMapper;

@Repository
public class MongoOrderEventRepository implements OrderEventRepository {

  private final SpringDataOrderEventRepo repo;
  private final ObjectMapper objectMapper;

  public MongoOrderEventRepository(SpringDataOrderEventRepo repo, ObjectMapper objectMapper) {
    this.repo = repo;
    this.objectMapper = objectMapper;
  }

  @Override
  public void append(DomainEvent event) {
    repo.save(OrderEventMapper.toDoc(event, objectMapper));
  }

  @Override
  public List<DomainEvent> findByAggregateId(OrderId orderId) {
    return repo.findByAggregateIdOrderByVersionAsc(orderId.value().toString()).stream()
        .map(d -> OrderEventMapper.toDomain(d, objectMapper))
        .toList();
  }

  @Override
  public int nextVersion(OrderId orderId) {
    return Math.toIntExact(repo.countByAggregateId(orderId.value().toString()) + 1);
  }
}
