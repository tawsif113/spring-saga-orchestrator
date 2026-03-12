package com.example.saga.order.infra.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.Instant;
import java.util.List;

public interface SpringDataOrderEventRepo extends MongoRepository<OrderEventDocument, String> {
  List<OrderEventDocument> findByAggregateIdOrderByVersionAsc(String aggregateId);
  long countByAggregateId(String aggregateId);
  List<OrderEventDocument> findTop100ByPublishedAtIsNullOrderByOccurredAtAscVersionAsc();

  @Query("{ '_id': ?0, 'aggregateId': ?1 }")
  @Update("{ '$set': { 'publishedAt': ?2 } }")
  long markPublished(String eventId, String aggregateId, Instant publishedAt);
}
