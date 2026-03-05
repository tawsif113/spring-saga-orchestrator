package com.example.saga.common.events;

import java.util.UUID;

public record PaymentAuthorizedPayload(
    UUID orderId,
    UUID paymentId,
    String authCode
) {
}
