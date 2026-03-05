package com.example.saga.inventory.infra.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataStockItemRepository extends MongoRepository<StockItemDocument, String> {
}