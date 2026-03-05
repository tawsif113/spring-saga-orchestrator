package com.example.saga.common.events;

import java.util.UUID;

public record InventoryRejectedPayload(UUID orderId, String reason) {
}
