package com.example.saga.common.events;

import java.util.UUID;

public record PaymentRejectedPayload(
    UUID orderId,
    UUID paymentId,
    String reason
) {
}
