package com.example.saga.inventory.domain;

import com.example.saga.common.Ids.ProductId;

import java.util.Optional;

public interface StockItemRepository {
  void save(StockItem stockItem);
  Optional<StockItem> findByProductId(ProductId productId);
}