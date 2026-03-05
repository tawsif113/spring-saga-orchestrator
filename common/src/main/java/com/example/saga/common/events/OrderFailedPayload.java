package com.example.saga.common.events;

import java.util.UUID;

public record OrderFailedPayload(UUID orderId, String reason) {
}
