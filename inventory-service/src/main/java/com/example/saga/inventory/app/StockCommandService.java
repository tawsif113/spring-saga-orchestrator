package com.example.saga.inventory.app;

import com.example.saga.common.Ids.ProductId;
import com.example.saga.inventory.domain.StockItem;
import com.example.saga.inventory.domain.StockItemRepository;
import org.springframework.stereotype.Service;

@Service
public class StockCommandService {

  private final StockItemRepository repo;

  public StockCommandService(StockItemRepository stockItemRepository) {
    this.repo = stockItemRepository;
  }

  public StockItem get(ProductId productId) {
    return repo.findByProductId(productId)
        .orElseThrow(() -> new IllegalArgumentException("Product not found"));
  }

  public StockItem create(ProductId productId) {
    repo.findByProductId(productId).ifPresent(existing -> {
      throw new IllegalStateException(
          String.format("Product with id %s already exists", productId));
    });
    StockItem stockItem = new StockItem(productId);
    repo.save(stockItem);
    return stockItem;
  }

  public StockItem addStock(ProductId productId, int qty) {
    StockItem stockItem = get(productId);
    stockItem.addStock(qty);
    repo.save(stockItem);
    return stockItem;
  }

  public StockItem reserve(ProductId productId, int qty) {
    StockItem stockItem = get(productId);
    stockItem.reserve(qty);
    repo.save(stockItem);
    return stockItem;
  }

  public StockItem release(ProductId productId, int qty) {
    StockItem stockItem = get(productId);
    stockItem.release(qty);
    repo.save(stockItem);
    return stockItem;
  }
}
