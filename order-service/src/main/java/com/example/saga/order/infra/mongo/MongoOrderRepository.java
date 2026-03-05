package com.example.saga.order.infra.mongo;

import com.example.saga.common.Ids.OrderId;
import com.example.saga.order.domain.Order;
import com.example.saga.order.domain.OrderRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MongoOrderRepository implements OrderRepository {

  private final SpringDataOrderRepo repo;

  public MongoOrderRepository(SpringDataOrderRepo repo) {
    this.repo = repo;
  }

  @Override
  public void save(Order order) {
    repo.save(OrderMapper.toDoc(order));
  }

  @Override
  public Optional<Order> findById(OrderId id) {
    return repo.findById(id.value().toString()).map(OrderMapper::toDomain);
  }
}
