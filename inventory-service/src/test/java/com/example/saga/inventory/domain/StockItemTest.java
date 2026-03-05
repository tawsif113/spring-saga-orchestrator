package com.example.saga.inventory.domain;

import com.example.saga.common.Ids.ProductId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockItemTest {

  @Test
  void commitReservationReducesReservedQtyOnly() {
    StockItem item = new StockItem(new ProductId(UUID.randomUUID()));
    item.addStock(10);
    item.reserve(6);

    item.commitReservation(4);

    assertEquals(4, item.availableQty());
    assertEquals(2, item.reservedQty());
  }

  @Test
  void commitReservationCannotExceedReservedQty() {
    StockItem item = new StockItem(new ProductId(UUID.randomUUID()));
    item.addStock(3);
    item.reserve(2);

    assertThrows(IllegalStateException.class, () -> item.commitReservation(3));
  }
}
