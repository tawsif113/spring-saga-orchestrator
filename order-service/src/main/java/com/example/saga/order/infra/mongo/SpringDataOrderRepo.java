package com.example.saga.order.infra.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataOrderRepo extends MongoRepository<OrderDocument, String> {}