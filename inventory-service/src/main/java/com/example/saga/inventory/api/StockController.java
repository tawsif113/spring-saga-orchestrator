package com.example.saga.inventory.api;

import com.example.saga.common.Ids.ProductId;
import com.example.saga.inventory.app.StockCommandService;
import com.example.saga.inventory.domain.StockItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/stock")
public class StockController {

  private final StockCommandService service;

  public StockController(StockCommandService service) {
    this.service = service;
  }

  public record CreateStockRequest(@NotNull UUID productId) {}
  public record QtyRequest(@Min(1) int qty) {}
  public record StockResponse(UUID productId, int availableQty, int reservedQty) {}

  @PostMapping
  public StockResponse create(@RequestBody CreateStockRequest req) {
    return toResponse(service.create(new ProductId(req.productId())));
  }

  @PostMapping("/{productId}/add")
  public StockResponse addStock(@PathVariable UUID productId, @RequestBody QtyRequest req) {
    return toResponse(service.addStock(new ProductId(productId), req.qty()));
  }

  @PostMapping("/{productId}/reserve")
  public StockResponse reserve(@PathVariable UUID productId, @RequestBody QtyRequest req) {
    return toResponse(service.reserve(new ProductId(productId), req.qty()));
  }

  @PostMapping("/{productId}/release")
  public StockResponse release(@PathVariable UUID productId, @RequestBody QtyRequest req) {
    return toResponse(service.release(new ProductId(productId), req.qty()));
  }

  @GetMapping("/{productId}")
  public StockResponse get(@PathVariable UUID productId) {
    return toResponse(service.get(new ProductId(productId)));
  }

  private static StockResponse toResponse(StockItem stockItem) {
    return new StockResponse(
        stockItem.productId().value(),
        stockItem.availableQty(),
        stockItem.reservedQty()
    );
  }
}