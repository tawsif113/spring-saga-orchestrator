package com.example.saga.common.events;

import java.util.UUID;

public record OrderConfirmedPayload(UUID orderId) {
}
