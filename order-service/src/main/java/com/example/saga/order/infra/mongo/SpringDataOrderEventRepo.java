package com.example.saga.order.infra.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SpringDataOrderEventRepo extends MongoRepository<OrderEventDocument, String> {
  List<OrderEventDocument> findByAggregateIdOrderByVersionAsc(String aggregateId);
  long countByAggregateId(String aggregateId);
  List<OrderEventDocument> findTop100ByPublishedAtIsNullOrderByOccurredAtAscVersionAsc();
}
