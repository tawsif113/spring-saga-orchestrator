package com.example.saga.inventory.infra.mongo;

import com.example.saga.common.Ids.ProductId;
import com.example.saga.inventory.domain.StockItem;
import com.example.saga.inventory.domain.StockItemRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MongoStockItemRepository implements StockItemRepository {

  private final SpringDataStockItemRepository repo;

  public MongoStockItemRepository(SpringDataStockItemRepository repo) {
    this.repo = repo;
  }

  @Override
  public void save(StockItem stockItem) {
    repo.save(StockItemMapper.toDocument(stockItem));
  }

  @Override
  public Optional<StockItem> findByProductId(ProductId productId) {
    return repo.findById(productId.value().toString())
        .map(StockItemMapper::toDomain);
  }
}