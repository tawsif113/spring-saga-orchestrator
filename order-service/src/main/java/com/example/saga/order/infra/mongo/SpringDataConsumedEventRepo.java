package com.example.saga.order.infra.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataConsumedEventRepo extends MongoRepository<ConsumedEventDocument, String> {
}
