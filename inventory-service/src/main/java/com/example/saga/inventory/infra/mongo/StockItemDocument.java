package com.example.saga.inventory.infra.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "stock_items")
public class StockItemDocument {
  @Id
  public String productId;

  public int availableQty;
  public int reservedQty;
}