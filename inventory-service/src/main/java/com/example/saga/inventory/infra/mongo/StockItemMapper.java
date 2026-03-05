package com.example.saga.inventory.infra.mongo;

import com.example.saga.common.Ids.ProductId;
import com.example.saga.inventory.domain.StockItem;

import java.util.UUID;

final class StockItemMapper {

  static StockItemDocument toDocument(StockItem stockItem) {
    StockItemDocument d = new StockItemDocument();
    d.productId = stockItem.productId().value().toString();
    d.availableQty = stockItem.availableQty();
    d.reservedQty = stockItem.reservedQty();
    return d;
  }

  static StockItem toDomain(StockItemDocument d) {
    return StockItem.rehydrate(
        new ProductId(UUID.fromString(d.productId)),
        d.availableQty,
        d.reservedQty
    );
  }

  private StockItemMapper() {
  }
}